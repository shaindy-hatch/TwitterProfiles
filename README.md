# TwitterProfiles

RunScoringModel.java contains functions that run the NameScorer and LocationScorer algorithms. 

WriteToDB.java is a background process to make the number of allowed calls to Twitter API for each timeframe (15 minutes)

src/main/config.properties file needs to be filled with user's credential values to connect to hatch database

Twitter Profile Project – A confidence or accuracy scoring model to determine whether a twitter handle belongs to a donor prospect.
The goal:
1. To verify the accuracy of existing twitter handles associated with donor prospects.  
 - The model will give a confidence score determining how likely a twitter handle in our database corresponds to its associated donor

2. Find missing twitter ids for donor prospects.

Since the Twitter API has strict rate limits on API calls a database was created to store accumulated data retrieved from multiple calls.
A twitter user table was set up to store twitter profiles and all donor prospects in the database that contained a twitter id where added. As a result, we can compare the prospect data in the hatch database with twitter data, test the algorithm, and score prospects twitter ids.

Another table was created to store users and their followers. Since prospects of an organization often follow one another on Twitter, keeping track of a prospect's followers enables us to identify missing Twitter information. Using this larger dataset, we can use the model to determine whether twitter profiles correspond to prospects in our database by scoring the accuracy of a match. 

