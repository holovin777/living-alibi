extends RefCounted
## Populate the existing F action menu using the shared Java workspace.
static func label(parent: VBoxContainer,value: String) -> void:
    var item:=Label.new()
    item.text=value
    item.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    parent.add_child(item)

static func action_button(parent: VBoxContainer,value: String,callback: Callable) -> void:
    var btn:=Button.new()
    btn.text=value
    btn.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    btn.pressed.connect(callback)
    parent.add_child(btn)

static func build(parent: VBoxContainer,work: Dictionary,submit: Callable) -> void:
    var college=work.get("college")
    if college is Dictionary and bool(college.get("atCampus",false)):
        label(parent,"COLLEGE / UNIVERSITÀ")
        for course in college.get("courses",[]):
            label(parent,"%s · %s/%s · %s" % [course.get("label",""),course.get("completed",0),course.get("required",0),"Diploma" if course.get("diploma",false) else course.get("career","")])
    var workspace=work.get("workspace")
    if workspace is Dictionary:
        label(parent,"WORKSPACE · "+str(workspace.get("company","Borgo Digital")))
        if not bool(workspace.get("onboarded",false)):label(parent,str(workspace.get("intro","")))
        var career=workspace.get("career")
        if career is Dictionary:
            label(parent,"%s · XP %s · technical %s · releases %s · audits %s" % [career.get("rank",""),career.get("xp",0),career.get("technical",0),career.get("releases",0),career.get("audits",0)])
            if career.get("nextRank")!=null:
                label(parent,"Next: %s · XP %s · technical %s · audits %s" % [career.get("nextRank"),career.get("requiredXp",0),career.get("requiredTechnical",0),career.get("requiredAudits",0)])
        for project in work.get("projects",[]):
            label(parent,"%s · %s · €%s\n%s\n%s" % [project.get("title",""),project.get("phase",""),project.get("wage",0),project.get("branch",""),project.get("explanation","")])
        var devops=workspace.get("devops")
        if devops is Dictionary:label(parent,"DEVOPS · "+str(devops.get("status",""))+"\n"+str(devops.get("guide",""))+"\n"+str(devops.get("output","")))
        var company=workspace.get("companyStatus")
        if company is Dictionary:
            label(parent,"IT COMPANY · "+str(company.get("guide","")))
            if bool(company.get("registered",false)):
                label(parent,"Company cash €%s · lease until day %s · contracts %s · tax €%s" % [company.get("cash",0),company.get("rentUntil",0),company.get("contracts",0),company.get("taxPaid",0)])
                var staff: Dictionary=company.get("staff",{})
                var employee=staff.get("employee")
                if employee is Dictionary:label(parent,"Staff: "+str(employee.get("name",""))+" · "+str(employee.get("activity","")))
    for choice in work.get("actions",[]):
        action_button(parent,str(choice.get("label","")),submit.bind(str(choice.get("id",""))))
    for entry in work.get("inputs",[]):
        label(parent,str(entry.get("label",""))+"\n"+str(entry.get("prompt","")))
        var editor:=TextEdit.new()
        editor.custom_minimum_size=Vector2(0,90)
        editor.text=str(entry.get("starter",""))
        editor.placeholder_text="Type your solution / Scrivi la soluzione"
        editor.wrap_mode=TextEdit.LINE_WRAPPING_BOUNDARY
        parent.add_child(editor)
        var feedback:=Label.new()
        feedback.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
        parent.add_child(feedback)
        var prefix:=str(entry.get("prefix",""))
        var limit:=int(entry.get("maxLength",180))
        action_button(parent,"Submit / Invia",func():
            var answer:=editor.text.strip_edges()
            if answer.is_empty() or answer.length()>limit:
                feedback.text="Enter 1–%s characters." % limit
                return
            submit.call(prefix+answer))
