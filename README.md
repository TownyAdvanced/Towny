<p align="center">
<img src="http://towny.palmergames.com/wp-content/uploads/2013/01/townylogo.png" height="155" width="153">
</p>

# Towny Advanced
### Developed by [LlmDl](https://github.com/LlmDl)

I took over from ElgarL after MC 1.8 was released. Past developers have included: Shadeness, FuzzieWuzzie, ElgarL. 
With help coming from other developers from time to time including dumptruckman, ole8pie, SwearWord, gravypod, andrewyunt and more.

Towny is one of the oldest still-in-development plugins for Minecraft. It was created by Shadeness for the now-defunct server platform called hMod.
It is the second-oldest land protection plugin to become popular for Minecraft, having been beaten by WorldGuard by just a couple months.

Releases, Dev builds and all other plugins I maintain/dev are available from...

* http://palmergames.com/towny

#### Connect
If you need help, join us in our [IRC channel #towny](http://webchat.esper.net/?channels=towny) on the Esper.net network.
If you are a server admin that wants to get cutting edge updates on the development of the plugin and want to help test things before they become public,
join us in our [Discord server]( https://discord.gg/gnpVs5m )

#### Contributing
If you'd like to contribute to the Towny code, see the [Contributing.md](https://github.com/LlmDl/Towny/blob/master/.github/CONTRIBUTING.MD).

#### Licensing

Towny is licensed under the [Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported (CC BY-NC-ND 3.0) License ](http://creativecommons.org/licenses/by-nc-nd/3.0/)

We don't object to you making your own forks and builds but we do object to people being selfish, which is why we specify No Derivative Works.
If you want to modify the code to add some nice feature the least you can do is ask and submit a pull request to allow everyone to benefit from it.

#### Building
If you would like to build from a specific branch yourself, you can do so with either [Apache Ant](https://ant.apache.org/) or [Apache Maven](http://maven.apache.org/), depending on the age of the branch.

For building, open your terminal / command prompt and navigate to the Towny Directory (either extracted, or cloned).

- **Maven**

    - Run `mvn clean package` to generate the plugin in the `target` directory, within the Towny folder. 
    - Developers may use the following after setting up their github token [as shown here.](https://help.github.com/en/articles/configuring-apache-maven-for-use-with-github-package-registry#authenticating-to-github-package-registry).
        
```
  <repositories>
    <repository>
      <id>github-Towny</id>
      <url>https://maven.pkg.github.com/TownyAdvanced/Towny</url>
    </repository>   
  </repositories>
  <dependencies>                    
    <dependency>
      <groupId>com.palmergames.bukkit.towny</groupId>
      <artifactId>Towny</artifactId>
      <version>0.94.0.12</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>  
```

- **Ant** (_Deprecated_)

    - For older branches using the Ant build system, the main command to use would be: `ant clean jar`. This command will _exit_ the Towny directory, creating a lib folder alongside it. A Towny.jar file will be generated and placed within there.
    - _Note: As Ant is being deprecated, older branches may eventually not be able to be built against without modification of the `build.xml` file. We leave no guarantee that the file repo providing the dependencies will stay up._