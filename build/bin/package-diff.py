#!/usr/bin/python

import sys,os,re
from sets import Set

################################################################################
# Configuration
################################################################################
downloadsite = "http://vaadin.com/download"
latestfile   = "/release/6.6/LATEST"

JAPIZE     = "japize"
JAPICOMPAT = "japicompat"

################################################################################
# Utility Functions
################################################################################
def command(cmd, dryrun=0):
	if not dryrun:
		if os.system(cmd):
			print "Command '%s' failed, exiting." % (cmd)
			sys.exit(1)
	else:
		print "Dry run - not executing."

################################################################################
# List files in an archive.
################################################################################
def listZipFiles(archive):
    pin = os.popen("unzip -l -qq %s | cut -c 29- | sort" % (archive), "r")
    files = map(lambda x: x.strip(), pin.readlines())
    pin.close()

    cleanedfiles = []
    for file in files:
        # Remove archive file name from the file names
        slashpos = file.find("/")
        if slashpos != -1:
            cleanedname = file[slashpos+1:]
        else:
            cleanedname = file

        # Purge GWT compilation files.
        if cleanedname.find(".cache.html") != -1:
            continue
        
        cleanedfiles.append(cleanedname)

    return cleanedfiles

################################################################################
# Difference of two lists of files
################################################################################
def diffFiles(a, b):
	diff = Set(a).difference(Set(b))
	difffiles = []
	for item in diff:
		difffiles.append(item)
	difffiles.sort()
	return difffiles

################################################################################
# Lists files inside a Zip file (a JAR)
################################################################################
def listJarFiles(jarfile):
	# Read the jar content listing
	pin = os.popen("unzip -ql %s" % jarfile, "r")
	lines = map(lambda x: x[:-1], pin.readlines())
	pin.close()

	# Determine the position of file names
	namepos = lines[0].find("Name")
	files = []
	for i in xrange(2, len(lines)-2):
		filename = lines[i][namepos:]
		files.append(filename)

	return files

################################################################################
# Lists files inside a Vaadin Jar inside a ZIP
################################################################################

# For Vaadin 6.3 Zip
def listZipVaadinJarFiles(zipfile, vaadinversion):
    jarfile = "vaadin-%s/WebContent/vaadin-%s.jar" % (vaadinversion, vaadinversion)
    extractedjar = "/tmp/vaadinjar-tmp-%d.jar" % (os.getpid())
    zipcmd = "unzip -p %s %s > %s " % (zipfile, jarfile, extractedjar)
    command (zipcmd)
    files = listJarFiles(extractedjar)
    command ("rm %s" % (extractedjar))
    return files

################################################################################
# JAPI - Java API Differences
################################################################################
def japize(version, zipfile):
    jarfile = "/tmp/vaadin-tmp.jar"
    packagedjar = "vaadin-%s/WebContent/vaadin-%s.jar" % (version, version)
    command ("unzip -p %s %s > %s " % (zipfile, packagedjar, jarfile))

    cmd = "%s as %s apis %s +com.vaadin, $JAVA_HOME/jre/lib/rt.jar lib/core/**/*.jar 2>/dev/null" % (JAPIZE, version, jarfile)
    command (cmd)

    return "%s.japi.gz" % (version)

def japicompat(japi1, japi2):
    cmd = "%s -q %s %s" % (JAPICOMPAT, japi1, japi2)
    pin = os.popen(cmd, "r")
    lines = "".join(pin.readlines())
    pin.close()
    return lines

################################################################################
#
################################################################################

# Download the installation package of the latest version
wgetcmd = "wget -q -O - %s" % (downloadsite+latestfile)
pin = os.popen(wgetcmd, "r")
latestdata = pin.readlines()
pin.close()

latestversion  = latestdata[0].strip()
latestpath     = latestdata[1].strip()
latestURL      = downloadsite + "/" + latestpath + "/"

latestfilename  = "vaadin-%s.zip" % (latestversion)
latestpackage   = latestURL + latestfilename
locallatestpackage = "/tmp/%s" % (latestfilename)

print "Latest version:      %s" % (latestversion)
print "Latest version path: %s" % (latestpath)
print "Latest version URL:  %s" % (latestURL)

# Check if it already exists
try:
	os.stat(locallatestpackage)
	print "Latest package already exists in %s" % (locallatestpackage)
	# File exists
except OSError:
	# File does not exist, get it.
	print "Downloading latest release package %s to %s" % (latestpackage, locallatestpackage)
	wgetcmd = "wget -q -O %s %s" % (locallatestpackage, latestpackage)
	command (wgetcmd)

# List files in latest version.
latestfiles  = listZipFiles(locallatestpackage)

# List files in built version.
builtversion = sys.argv[1]
builtpackage = "build/result/vaadin-%s.zip" % (builtversion)
builtfiles = listZipFiles(builtpackage)

# Report differences

print "\n--------------------------------------------------------------------------------\nVaadin ZIP differences"

# New files
newfiles = diffFiles(builtfiles, latestfiles)
print "\n%d new files:" % (len(newfiles))
for item in newfiles:
	print item

# Removed files
removed = diffFiles(latestfiles, builtfiles)
print "\n%d removed files:" % (len(removed))
for item in removed:
	print item

print "\n--------------------------------------------------------------------------------\nVaadin JAR differences"

latestJarFiles = listZipVaadinJarFiles(locallatestpackage, latestversion)
builtJarFiles  = listZipVaadinJarFiles(builtpackage,       builtversion)

# New files
newfiles = diffFiles(builtJarFiles, latestJarFiles)
print "\n%d new files:" % (len(newfiles))
for item in newfiles:
	print item

# Removed files
removed = diffFiles(latestJarFiles, builtJarFiles)
print "\n%d removed files:" % (len(removed))
for item in removed:
	print item

print "\n--------------------------------------------------------------------------------\nVaadin API differences"
oldjapi = japize(latestversion, locallatestpackage)
newjapi = japize(builtversion, builtpackage)

print "\n--------------------------------------------------------------------------------\nLost API features\n"
japidiff1 = japicompat(oldjapi, newjapi)
print japidiff1

print "\n--------------------------------------------------------------------------------\nNew API features\n"
japidiff2 = japicompat(newjapi, oldjapi)
print japidiff2

# Purge downloaded package
command("rm %s" % (locallatestpackage))
