sed -i "s/REMAP_MIXINS = false/REMAP_MIXINS = true/g" /home/michael/Programs/IdeaProjects/Cold-Sweat-Legacy/src/main/java/dev/momostudios/coldsweat/ColdSweat.java
chmod +x gradlew
./gradlew build
sed -i "s/REMAP_MIXINS = true/REMAP_MIXINS = false/g" /home/michael/Programs/IdeaProjects/Cold-Sweat-Legacy/src/main/java/dev/momostudios/coldsweat/ColdSweat.java
