# dailybrainy
Daily design challenges for kids

## strategy-firebase

Firebase's remote database will be the master source of truth.
ViewModels, each associated with a screen, will query Firebase on demand, whenever
the ViewModel's lifecycle begins.