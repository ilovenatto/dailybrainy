# dailybrainy
Daily design challenges for kids

## strategy-roomdb
This was an attempt to use Android Room and Firebase together.
Room would act as the local cache and Firebase the remote source of truth.

I found that it got too complicated syncing the two data sources together, 
especially with multiple devices writing to the Firebase db (imagine different
players, each on their own device).

Trying *strategy-firebase* now, which you can find in it's own branch.