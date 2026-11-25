package com.holovin.npclife;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Restricted annotation exercise and real SQLite SELECT evaluation on disposable fixtures. */
final class RetailTicketLife {
    private RetailTicketLife(){}
    private static final Pattern MAPPING=Pattern.compile("\\s*@GetMapping\\s*\\(\\s*(?:value\\s*=\\s*)?\"/retail/catalog\"\\s*\\)\\s*");
    private static final Pattern QUERY=Pattern.compile("SELECT\\s+id\\s+FROM\\s+products\\s+WHERE\\s+stock\\s*(?:<=|>=|<>|!=|=|<|>)\\s*-?\\d{1,3}\\s+ORDER\\s+BY\\s+id(?:\\s+ASC)?\\s*;?",Pattern.CASE_INSENSITIVE);
    static void verifyMapping(String source){
        if(!MAPPING.matcher(source).matches())
            throw new IllegalArgumentException("Use a read-only GET method mapping with the relative path, without repeating /api/v1.");
    }
    static void verifyQuery(String source){
        String query=source.strip();
        if(!QUERY.matcher(query).matches())
            throw new IllegalArgumentException("Use one SELECT id FROM products with a stock comparison and ORDER BY id. Other statements are not supported.");
        check(query,new int[]{0,3,2,10},List.of("bread","eggs"));
        check(query,new int[]{3,1,5,0},List.of("milk","water"));
        check(query,new int[]{3,3,3,3},List.of());
        check(query,new int[]{1,0,2,1},List.of("bread","eggs","milk","water"));
    }
    private static void check(String query,int[] stocks,List<String> expected){
        try(var db=DriverManager.getConnection("jdbc:sqlite::memory:");var statement=db.createStatement()){
            statement.execute("CREATE TABLE products(id TEXT PRIMARY KEY, stock INTEGER NOT NULL, price INTEGER NOT NULL)");
            String[] ids={"bread","water","eggs","milk"};
            try(var insert=db.prepareStatement("INSERT INTO products VALUES(?,?,?)")){
                for(int i=ids.length-1;i>=0;i--){
                    insert.setString(1,ids[i]);insert.setInt(2,stocks[i]);insert.setInt(3,i+1);insert.executeUpdate();
                }
            }
            statement.execute("PRAGMA query_only=ON");
            List<String> actual=new ArrayList<>();
            try(var result=statement.executeQuery(query)){while(result.next())actual.add(result.getString(1));}
            if(!actual.equals(expected))throw new IllegalArgumentException("SQL test failed: expected "+expected+", got "+actual+".");
        }catch(SQLException e){throw new IllegalArgumentException("SQL exercise failed: "+e.getMessage(),e);}
    }
}
