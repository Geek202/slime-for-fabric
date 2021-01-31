# slime-for-fabric
An implementation of [the SLIME-modified format](https://gist.github.com/Geek202/b30aa8362f5dcc635f6703ecbc88f336) on Fabric 1.16.5
To use the mod as an API, add [jitpack](https://jitpack.io/) as a repository in your `build.gradle`:
```gradle
repositories {
    maven {
        name = "Jitpack"
        url = "https://jitpack.io/"
    }
}
```
Then add the dependency in the dependencies block like so.
```gradle
dependenceis {
    // [...] Minecraft, mappings, etc.
    modImplementation "com.github.Geek202:slime-for-fabric:1.0"
}
```
