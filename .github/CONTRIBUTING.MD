# Contributing to Towny
   
    
## Contributing Code    
Code contributions are welcome and are made via GitHub's Pull Request. 
You must fork the Towny repository (this) and then open it via your IDE.   
You can ask for any information/help on your idea/contribution in the [Towny IRC](http://webchat.esper.net/?channels=towny).    
Please make sure to read our [License](https://github.com/LlmDl/Towny/blob/master/LICENSE.md). If you do not agree with the terms and conditions of our license, please do not contribute any code.    

# Pull & Code Requirements    
## Pull-Request Requirements
1. Keep your commit message(s) self-explanitory, description should be a brief explanation and include an issue reference (if applicable.).   
2. Pull Requests should be simple and straight-forward. One pull request for one feature. Changed Files should only show a difference where code has been removed or added, no re-formatting of entire files.   
3. Test everything you've touched before pull-requesting. (Exception being language files, we don't expect you to check all lang strings.)    

## Code Requirements    
1. You must use Tabs (Size 4 Spaces).    
2. Keep your environment in Java 8,   
3. Avoid using Wildcard imports (It's not preferred, but it's allowed.)   
4. Don't run IDE Formatters over entire class files, unless it's a new class-file you're implementing.   
   - This is done to avoid us changing the lines everytime, each IDE is different so we don't have a constant here and there.
   - This also keeps file compares on github simple.
5. Do not use NMS (Net Minecraft Server), Spigot or Craftbukkit code. Towny uses the Bukkit API only.
6. Reflection should be a last resort.   
7. No Packet-Handling (Netty or ProtcolLib)   
8. Do not add any libaries into Towny, without asking first.
9. Do not remove the ANT build system used by Towny.
   - Towny uses ANT and not maven or gradle.

## Updating Language Files
The language files are stored in the root-package of the project src/com/palmergames/"language".yml    
To update / change an existing language file, fork this repo and then edit the "language".yml in your fork.    
Pull-Request your updated version. Please try to keep the title simple and include the version number:    
e.g "German Translation v0.42"    
If you are creating a new language file, feel-free to use either the english translation e.g "norwegian.yml" or the global placeholder e.g "es-mx"    
If you are editing a language which contains the '(apostrophe) character such as French, please make sure you run it all through a YAML Parser,    
You have to double up the '' when you want one ' to appear.    
