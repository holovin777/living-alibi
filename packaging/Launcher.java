import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Runs the bundled server and native Godot export without system Java or Maven. */
public final class Launcher {
    private static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");
    public static void main(String[] args) {
        try {
            if (args.length != 0 && !(args.length == 1 && args[0].equals("--smoke"))) {
                throw new IllegalArgumentException("Only --smoke is supported.");
            }
            run(args.length == 1);
        } catch (Exception ex) {
            System.err.println("Living Alibi: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static Path dataDir() {
        String override = System.getenv("LIVING_ALIBI_DATA_DIR");
        if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath();
        String base = System.getenv(WINDOWS ? "LOCALAPPDATA" : "XDG_DATA_HOME");
        if (base == null || base.isBlank()) base = WINDOWS
            ? Path.of(System.getProperty("user.home"), "AppData", "Local").toString()
            : Path.of(System.getProperty("user.home"), ".local", "share").toString();
        return Path.of(base, WINDOWS ? "LivingAlibi" : "living-alibi").toAbsolutePath();
    }

    private static void run(boolean smoke) throws Exception {
        Path root = Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        Path data = dataDir();
        // Smoke runs must never use the player's real save.
        if (smoke && System.getenv("LIVING_ALIBI_DATA_DIR") == null)
            throw new IOException("Set LIVING_ALIBI_DATA_DIR to an isolated test folder.");
        Files.createDirectories(data);
        try (FileChannel channel = FileChannel.open(data.resolve("game.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.tryLock()) {
            if (lock == null) throw new IOException("The same saved world is already open.");
            launch(root, data, smoke);
        }
    }

    private static void launch(Path root, Path data, boolean smoke) throws Exception {
        Path java = root.resolve("runtime/bin/" + (WINDOWS ? "java.exe" : "java"));
        Path game = root.resolve(WINDOWS ? "LivingAlibi.exe" : "LivingAlibi.x86_64");
        Path serverJar = root.resolve("server.jar");
        for (Path path : List.of(java, game, serverJar))
            if (!Files.isRegularFile(path)) throw new IOException("Incomplete game package: " + path);
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        Process server = new ProcessBuilder(java.toString(), "-jar", serverJar.toString(),
                "--server.address=127.0.0.1", "--server.port=" + port,
                "--living-alibi.database=" + data.resolve("world.db"))
            .directory(root.toFile()).redirectErrorStream(true)
            .redirectOutput(data.resolve("server.log").toFile()).start();
        Process[] client = {null};
        Runnable cleanup = () -> {
            stop(client[0]);
            stop(server);
        };
        Thread hook = new Thread(cleanup, "living-alibi-shutdown");
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/world"))
                .timeout(Duration.ofSeconds(2)).GET().build();
            boolean ready = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
            while (server.isAlive() && System.nanoTime() < deadline) {
                try {
                    var response = http.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 && response.body().contains("\"player\"") && server.isAlive()) {
                        ready = true; break;
                    }
                } catch (IOException ignored) {}
                Thread.sleep(250);
            }
            if (!ready) throw new IOException("Server failed to start; see " + data.resolve("server.log"));
            var command = new ArrayList<>(List.of(game.toString()));
            if (smoke) command.addAll(List.of("--headless", "--max-fps", "60", "--quit-after", "180", "--", "--packaged-smoke"));
            var builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true)
                .redirectOutput(data.resolve("game.log").toFile());
            builder.environment().put("LIVING_ALIBI_LOCAL_PORT", Integer.toString(port));
            client[0] = builder.start();
            if (smoke && !client[0].waitFor(45, TimeUnit.SECONDS))
                throw new IOException("Packaged game smoke timed out.");
            int exit = client[0].waitFor();
            if (exit != 0) throw new IOException("Game exited with " + exit + "; see game.log");
            if (smoke) {
                String log = Files.readString(data.resolve("game.log"));
                if (log.contains("SCRIPT ERROR") || log.contains("Parse Error") || log.contains("Failed loading resource"))
                    throw new IOException("Exported Godot game reported script/resource errors.");
                if (!log.contains("PACKAGED_WORLD_CONNECTED")) throw new IOException("Exported game never received the world snapshot.");
                if (!Files.isRegularFile(data.resolve("world.db"))) throw new IOException("No world database created.");
                System.out.println("Packaged game smoke passed.");
            }
        } finally {
            cleanup.run();
            try { Runtime.getRuntime().removeShutdownHook(hook); } catch (IllegalStateException ignored) {}
        }
    }

    private static void stop(Process process) {
        if (process == null || !process.isAlive()) return;
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }
}
