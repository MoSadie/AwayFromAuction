---
title: Away from Auction | MoSadie
---

# WIP Page

# Description
Away from Auction is a Minecraft Forge mod that allows the user to view the [Hypixel Skyblock](https://hypixel.net) auction house remotely and be notified in-game whenever someone outbids you, bids on your auction, or one of your auctions is close to ending.

The full list of commands can be found by running the `/afa` command while in-game.

# Setup
0) Download and install this mod like any other Minecraft Forge mod.
1) Configure your Hypixel API key using one of these two methods:
    1) For people who don't already have an API key:
        1) Join the Hypixel Minecraft Server: `mc.hypixel.net`
        2) Type the command `/api`
        3) Write down the API key. (Just in case you need to manually input it.)
        4) Look for a chat message letting you know the key was automatically detected.
    2) For people who already have an API key:
        1) Run the command `/afa key <paste your key here>` while in any Minecraft world/server.
2) Use the `/afa` command to view the auction house!

{% if site.github.releases.size > 0 %}
# Versions:
{% for release in site.github.releases %}

| Version | Title | Changelog | Downloads |
| ------- | ----- | --------- | --------- |
| {% if release.prerelease %} Pre-release: {% endif %} {{release.tag_name}} | {{release.name}} | [Changelog]({{release.html_url}}) | {% for asset in release.assets %} [{{asset.name}}]({{asset.browser_download_url}}) <br> {% endfor %}|

<br>
{% endfor %}
{% endif %}