extends SceneTree
const Work=preload("res://scripts/GeoWorkPanel.gd")
func _initialize() -> void:
    _check.call_deferred()
func _check() -> void:
    var panel:=VBoxContainer.new()
    root.add_child(panel)
    Work.build(panel,{"college":null,"workspace":null,"projects":[],"actions":[{"id":"test","label":"Test"}],"inputs":[{"prefix":"test:","label":"Ticket","prompt":"Rule","starter":"return 0;","maxLength":180}]},func(_id: String):pass)
    var editors:=0
    for child in panel.get_children():
        if child is TextEdit:editors+=1
    if editors!=1:
        push_error("Workspace editor missing")
        quit(1)
        return
    print("GeoWorkSmoke: ALL CHECKS PASSED")
    quit(0)
