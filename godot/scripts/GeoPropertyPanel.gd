extends RefCounted
const Work=preload("res://scripts/GeoWorkPanel.gd")
static func build(parent: VBoxContainer,data: Dictionary) -> void:
    var garden=data.get("garden")
    if garden is Dictionary:
        Work.label(parent,"GARDEN / ORTO · growth %s/%s · rainwater %s/%s\nGreenhouse: %s · compost %s · scraps %s" % [garden.get("growth",0),garden.get("required",0),garden.get("rainwater",0),garden.get("rainCapacity",0),garden.get("greenhouse",false),garden.get("compost",0),garden.get("scraps",0)])
    var hens=data.get("hens")
    if hens is Dictionary:
        Work.label(parent,"HENS / GALLINE · %s/%s · health %s · eggs %s" % [hens.get("hens",0),hens.get("maxHens",0),hens.get("health",0),hens.get("eggs",0)])
    var orchard=data.get("orchard")
    if orchard is Dictionary:
        Work.label(parent,"ORCHARD / FRUTTETO · honey %s · wine due day %s" % [orchard.get("honey",0),orchard.get("wineDueDay",0)])
        for plant in orchard.get("plants",[]):
            Work.label(parent,"%s · %s · age %s · fruit %s" % [plant.get("label",""),plant.get("season",""),plant.get("age",0),plant.get("fruit",0)])
    var farm=data.get("farm")
    if farm is Dictionary:
        Work.label(parent,"FARM / FATTORIA · field: vegetables %s, rice %s\nBarn: vegetables %s, rice %s" % [farm.get("vegetablesInField",0),farm.get("riceInField",0),farm.get("vegetablesInBarn",0),farm.get("riceInBarn",0)])
    var rental=data.get("rental")
    if rental is Dictionary:
        Work.label(parent,"APPARTAMENTO VERDI · owned %s · listed %s\nTenant: %s · paid until day %s · revenue €%s" % [rental.get("owned",false),rental.get("listed",false),str(rental.get("tenantName","")),rental.get("paidUntilDay",0),rental.get("revenue",0)])
    var construction=data.get("construction")
    if construction is Dictionary:
        Work.label(parent,"VIA VERDI · stage %s/3 · active %s\nSteel %s · parts %s · labor reserve €%s" % [construction.get("stage",0),construction.get("enabled",false),construction.get("steel",0),construction.get("parts",0),construction.get("reserve",0)])
