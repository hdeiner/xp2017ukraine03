# TimeTeller
------------

This project is for the XP Days Ukraine 2017 conference, and represents the first state of code for the "Improving Your Organization’s Technical Prowess With Legacy Code Retreats" talk.

Please do not judge me on this code alone. ☺ 

This code is designed to be an example of legacy code that works, but is nothing that we are proud of and pretty hard to maintain.

#### The goal at this point is to:
* We can now start to make the code better.  First, let's make it readable and demonstrate that we understand what we're working with.
* So we'll tackle magic numbers first.  Magic numbers are defined by Steve McConnell in Code Complete as "literal numbers, such as 100 or 47524 that appear in the middle of a program without explanation."  We're going to replace them with named constants or global variables.  And we'll name them so they appear in the code with their names being the design intent that they are present for, rather than just their value.  And, when possible, we're going to use enumerations to group the magic numbers into the groups they belong in.
* For example, the first argument of getResult (we'll get that name soon, I promise you) used to be an int.  Now it's a TimeZone (an enumeration of LOCAL and UTC) in it's own little class under com.deinersoft.timeteller.  The code reads better already!
* While these transformations are relatively safe, it is VITAL to rerun the automated tests very often, to guard against such things as typos.
* It is common to see magic numbers show up in configuration to the code, and now is not a bad time to fix that.  I'm using config.properties to make that happen.  Note how the code in TimeTeller isn't necessarily shorter, but instead of just a bunch of characters saying what a particular value is, you see what it's all about and why it's used here.  For example, while we need to know the value for mail.smtp.port, having a 587 sitting in the code is a spectacularily poor way to document it.
* Best example of why variable names matters is how renaming i, j, and k in getResults to hour, minute, and second almost completely makes the way that the routine goes about documenting how the "fuzzy words" time string gets constructed.