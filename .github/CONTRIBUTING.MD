# Contributing to Towny
Thanks for deciding to contribute to Towny! We really appreciate contributions, but to maintain everything cool around here, we need to enforce some guidelines to keep the project as efficient as possible. :shipit:

Contributions are welcomed via GitHub Pull Requests.
You must fork this repository and then open it via your IDE.   
You can always ask for help on your contribution in the [Towny Discord](https://discord.gg/gnpVs5m).    

## Before contributing
- Make sure to read the [Towny license](https://github.com/TownyAdvanced/Towny/blob/master/LICENSE.md).
- By contributing, you agree to distribute your changes under the Towny license and all its terms.

If you do not agree with the terms of our license, do not contribute to the Towny project.

# About pull requests
1. Keep your commit messages self-explainatory, the description should be a brief explanation and include an issue reference if applicable.
2. Pull requests should be straight-forward and focus only on one feature. Changed files should only show a difference where code has been actually modified.
3. Test your changes before sending them in! You should look for bugs, test configuration values, commands, and anything you've touched!

# About Java code
> Towny uses an [.editorconfig](https://editorconfig.org) file. IDEs that have [native EditorConfig support](https://editorconfig.org/#pre-installed) will automatically follow these rules.
    
1. You must use tabs for indentation. 
2. Keep your environment in Java 16-17. The code will automatically be converted into code compatible with Java 8 by [Jabel](https://github.com/bsideup/jabel).[^jabel-warning]
3. Avoid using wildcard imports.
4. Do not format entire class files.   
   - All IDEs are different, we do this to keep consistency in the codebase.
   - This also keeps code differences in GitHub clean and simple.
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

Strings are formatted using [Java's `java.util.Formatter`](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/Formatter.html) allowing specification of variable order, and more.

[^jabel-warning]: Keep in mind that, while you will be able to use modern language features that way (like patterns), some standard library methods added after JDK 8 will not work **even after being processed** and cause "Cannot find symbol" errors when building (like `String#isBlank`).
[^nms]: The `net.minecraft.server` is the Minecraft server source package. While this allows for "lower-level" coding, it's also pretty hard to maintain due to its obfuscation, no cross-version compatibility, and possibility to corrupt data.
