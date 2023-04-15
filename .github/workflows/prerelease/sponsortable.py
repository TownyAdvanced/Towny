columns = 6
sponsorspath = "./staging/sponsors.txt"
tableopen = "<table align=center>"
tableheader = "<tr><th colspan=%s><h3>Towny Sponsors<br><h4>I want to give a big thank you to all of my sponsors:<br>These are the people that help to make Towny's support and development as active as it is,<br>and who supported me during this pre-release of Towny. </th></tr>" % columns
tablefooter1 = "<tr><td colspan=%s align=center>" % columns
tablefooter2 = "<h3><a href=https://github.com/sponsors/LlmDl>If you want to support the developer, become a sponsor.</a></h3>"
tablefooter3 = "(It's just like Patreon but instead 100% of your support goes to the developer.)"
tablefooter4 = "<h3>Plus there are <a href=https://github.com/LlmDl/SponsorPlugins/blob/main/README.md>Sponsor Plugins!</a></h3></td></tr>"
tableclose = "</table>"
rows = [tableopen, tableheader]
laterRows = [tablefooter1, tablefooter2, tablefooter3, tablefooter4, tableclose]

def readSponsorFileIntoList():
    myfile = open(sponsorspath, "r")
    sponsors = []
    while myfile:
        sponsorline = myfile.readline()
        sponsors.append(sponsorline.replace("\n",""))
        if sponsorline == "":
            break
    myfile.close() 
    return sponsors

def populateSponsorNames(sponsors):
    i = 0
    row = ""
    privateSponsors = 0
    # Parse over the sponsors, making them into html-formatted table rows.
    for sponsorName in sponsors:
        if sponsorName == "":
            break
        if sponsorName.startswith("*"):
            privateSponsors = privateSponsors + 1
            continue

        i += 1
        row += "<td>" + sponsorName + "</td>"
        if i % columns == 0: # A row has been filled, dump it to rows, clear row.
            rows.append("<tr>" + row + "</tr>")
            row = ""

    # The last line in the body has yet to be added.
    if (row == ""): # The Sponsors ended up being enough to fill out the full width, add a new line for the private sponsors
        row = "<td colspan=%s>and %s private sponsors.</td>" % (columns, privateSponsors)
    else: # There is a partial row left over, fill it with the private sponsor cell
        row += "<td colspan=%s>and %s private sponsors.</td>" % (columns - row.count("<td>"), privateSponsors)
    rows.append("<tr>" + row + "</tr>")

def writeFile(rows):
    file = open('./staging/sponsortable.txt','w')
    for row in rows:
        file.writelines(row + "\n")
    file.close

populateSponsorNames(readSponsorFileIntoList())
rows += laterRows
writeFile(rows)

# print(rows)
