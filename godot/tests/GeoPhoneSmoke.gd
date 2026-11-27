extends SceneTree
const Phone=preload("res://scripts/GeoPhone.gd")
func _initialize() -> void:
    _check.call_deferred()
func _check() -> void:
    var phone=Phone.new()
    root.add_child(phone)
    phone.open_phone({"day":1,"minute":600,"phone":{"report":{"available":true,"calls":[],"dialContacts":[],"messages":[],"meetings":[]},"contacts":[],"jobs":[{"title":"Technician","wage":38,"workplace":{"x":20,"z":30,"label":"Workshop"}}],"bank":{"personal":25},"bankLocation":{"x":1,"z":2},"actions":[]}})
    for tab in ["Calls","Messages","Contacts","Jobs","Bank","Meetings"]:
        phone._switch(tab)
        if phone.content.get_child_count()==0:
            push_error("Empty phone app "+tab)
            quit(1)
            return
    print("GeoPhoneSmoke: ALL CHECKS PASSED")
    quit(0)
