# Contributing to Towny
   
    
## Contributing Code    
Code contributions are welcome and are made via GitHub's Pull Request. 
You must fork the Towny repository (this) and then open it via your IDE.   
You can ask for any information/help on your idea/contribution in the [Towny Discord](https://discord.gg/gnpVs5m).    

* Please make sure to read our [License](https://github.com/TownyAdvanced/Towny/blob/master/LICENSE.md). If you do not agree with the terms and conditions of our license, please do not contribute any code.
* By making a pull request, you represent that you have the right to waive copyright and related rights to your contribution, and agree that all copyright and related rights in your contributions are waived, and you acknowledge that the TownyAdvanced organization has the copyright to use and modify your contribution under our [License](https://github.com/TownyAdvanced/Towny/blob/master/LICENSE.md) for perpetuity.

# Pull & Code Requirements    
## Pull-Request Requirements
1. Keep your commit message(s) self-explanitory, description should be a brief explanation and include an issue reference (if applicable.).   
2. Pull Requests should be simple and straight-forward. One pull request for one feature. Changed Files should only show a difference where code has been removed or added, no re-formatting of entire files.   
3. Test everything you've touched before pull-requesting. (Exception being language files, we don't expect you to check all lang strings.)    

## General Code Requirements
> With the inclusion of the Maven build system, Towny also makes use of a [EditorConfig](https://editorconfig.org) file. IDEs that support it should automatically follow parts of this section.
    
1. You must use Tabs (Size 4 Spaces).    
2. Keep your environment in Java 8,   
3. Avoid using Wildcard imports (It's not preferred, but it's allowed.)   
4. Don't run IDE Formatters over entire class files, unless it's a new class-file you're implementing.   
   - This is done to avoid us changing the lines everytime, each IDE is different so we don't have a constant here and there.
   - This also keeps file compares on github simple.
5. Do not use NMS (Net Minecraft Server) or Craftbukkit code. Towny uses the Spigot/Bukkit API only.
6. Reflection should be a last resort.   
7. No Packet-Handling (Netty or ProtcolLib)   

## Dependency Inclusion
For the sanity of the rest of us, please adhere to the following guidelines when attempting to
include a dependency or 3rd-party library.
1. Libraries need to be approved by the current maintainer before being merged. Preferably, ask before commiting.     
1. Prevent Class Not Found errors    
   - Do not reference a plugin in a loaded class, unless that plugin is enabled.
2. Do not embed your own plugins within Towny
   - Dependencies should be within the 'Provided' scope.
   - They will also need peer-review.
3. No "Black Box" or Obufscated dependencies.
   - All dependencies should have their source open and auditable.

## Updating Language Files
The language files are stored in the resources section of the project. (`resources/"language".yml`)    
To update / change an existing language file, fork this repo and then edit the "language".yml in your fork.    
Pull-Request your updated version. Please try to keep the title simple and include the version number:    
e.g "German Translation v0.42"    
If you are creating a new language file, feel-free to use either the english translation e.g "norwegian.yml" or the global placeholder e.g "es-mx"    
If you are editing a language which contains the '(apostrophe) character such as French, please make sure you run it all through a YAML Parser,    
You have to double up the '' when you want one ' to appear. It may help to do this anyways, as other characters may also require an escape sequence.    
