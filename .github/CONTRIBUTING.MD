# Contributing to Towny
Thanks for deciding to contribute to Towny! We really appreciate contributions, but to keep everything running smoothly and get contributions merged in a timely manner, we need to enforce some guidelines. :shipit:

Contributions are welcome via GitHub Pull Requests.
You must fork this repository and then open it via your IDE.   
You can always ask for help on your contribution in the [Towny Discord](https://discord.gg/gnpVs5m).    

## Before contributing
- Make sure to read the [Towny license](https://github.com/TownyAdvanced/Towny/blob/master/LICENSE.md).
- By opening an pull request, you:
    - represent that you have the right to waive copyright and related rights to your contributions,
    - agree that all copyright and related rights in your contributions will be waived,
    - acknowledge that the TownyAdvanced organization has the copyright to use and modify your contributions under the [Towny license](https://github.com/TownyAdvanced/Towny/blob/master/LICENSE.md) for perpetuity.

If you do not agree with the terms of our license, do not contribute to the Towny project.

# About pull requests
> Pull requests will have their commits squashed upon merge, and your various commits' messages will be used in the squashed commit. Take this into account when making your commits.

1. Keep your commit messages self-explanatory. Keep them short but informative!
2. Pull requests should address a single topic, or be as closed in scope as you can reasonably make it. Small pull requests are easier to review and thus, can result in a faster merge.
3. Pull request forms should be filled out to the best of your abilities; describe your changes and reference issues/pull requests whenever applicable.
4. Test your changes before sending them in! You should look for bugs, test configuration values, commands, and anything you've touched!

# About Java code
> Towny uses an [.editorconfig](https://editorconfig.org) file. IDEs that have [native EditorConfig support](https://editorconfig.org/#pre-installed) will automatically follow these rules.
    
1. You must use tabs for indentation. 
2. Keep your environment in Java 17.
3. Do not use wildcard imports.
4. Do not make unnecessary changes.[^changes]
   - All IDEs are different, we do this to keep consistency in the codebase.
   - This keeps code differences in GitHub clean and simple.
5. Do not use NMS (`net.minecraft.server`)[^nms], or reflection. We aim at only using the Spigot API.
6. Do not directly interact with the Minecraft protocol.
   - This includes the usage of ProtocolLib or Netty.

# About dependencies
For the sanity of the rest of us, and the maintainability of the project, please adhere to the following guidelines when attempting to include a dependency or an third party library.

1. Libraries need to be approved by the maintainer before being merged. We recommend you ask before commiting.     
2. Prevent `ClassNotFound` exceptions.
   - Do not reference a plugin in a loaded class, unless that plugin is enabled.
3. Do not add plugin support directly into Towny **unless** it can benefit the project (such as economy/permission bridges).
   - For Towny support, you should be using the [`TownyAPI`](https://github.com/TownyAdvanced/Towny/wiki/TownyAPI) instead.
   - Even if you believe it can benefit the project, please ask the maintainer first.
4. Do not embed dependencies directly into Towny.
   - All dependencies should use the `provided` scope instead. This means that the dependency will be provided by the plugin JAR (e.g. Vault), or the server JAR (e.g. Adventure).
5. Do not add closed source dependencies.
   - All dependencies should have open and auditable source code.

# About translation
All translation on the core Towny plugin is done via [Crowdin](https://crowdin.com/project/townyadvanced).

Strings are formatted using [Java's `java.util.Formatter`](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/Formatter.html) allowing specification of variable order, and more. This means that if you find that a line has multiple variables which would be in a different order in your language, you can change them to `%NUMBER$s` (replacing `NUMBER` with the variable index) to change their order. 

[^changes]: This includes formatting and whitespace changes.

[^nms]: The `net.minecraft.server` is the Minecraft server source package. While this allows for "lower-level" coding, it's also pretty hard to maintain due to its obfuscation, no cross-version compatibility, and possibility to corrupt data.
