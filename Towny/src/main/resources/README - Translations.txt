Towny has support multi-language support. Languages available can be found on the Crowdin page for 
TownyAdvanced. https://crowdin.com/project/townyadvanced

If you don't see your language available ask for it on the Towny discord or in an issue ticket.

                                  How Translation Works In Towny:                                  

Towny uses the Minecraft game client's locale to determine which language Towny's messages will 
appear in, as long as it is one of the languages on our Crowdin page. This means that player's 
determine which Language they see Towny's messages in. The player sets their Minecraft Language, in
their Minecraft Settings, and Towny reads what locale the client wants to see Towny using.
Example: If your MC client is in English, you will see Towny in English. If your MC client is in 
German you will see Towny in German.


The server admin can select the default language in the config.yml's language: setting. This is the
language that most console messages will appear in, as well as the language for players whose 
language is not natively supported with a translation file from Crowdin.


Server admins can override the default language files in two of ways:

- The towny\settings\lang\override\ folder contains one file by default, the global.yml. This file 
  can have any of the Towny language strings added to it, and it will override every language. This
  is very useful for changing the Towny prefix, or the colours of the status screens.

- Admins can override single languages by moving files from the towny\settings\lang\reference\ 
  folder into the towny\settings\lang\override\ folder. This is good for cases where you want a 
  slightly different translation or (for some reason) you would like to translate Towny locally 
  rather than on Crowdin.

                              !!! Finally, and most importantly: !!!

               Making any changes to the files in the towny\settings\lang\reference\
               folder will have no effect in-game. When Towny loads your changed 
               reference files will be over-written, undoing all your work! Move 
               them to the override folder before you begin!