# Kismet Calculator

Tells you whether an M7 bedrock chest is worth more than the kismet feather it would take to
reroll it. Fabric, Minecraft 26.1.2, client only.

## Commands

| command | what it does |
| --- | --- |
| `/kismet` | prints the working: reroll EV, the feather's cost, the handle term |
| `/kismet meter <0-100>` | your RNG meter progress towards Necron's Handle |
| `/kismet key <coins>` | override the key price; `0` goes back to the bazaar |
| `/kismet offers <true\|false>` | value drops at sell offers instead of instant sell |
| `/kismet overlay <true\|false>` | draw the panel at all |
| `/kismet breakdown <true\|false>` | list all six chests, or only the one being judged |
| `/kismet gamblingmode <true\|false>` | swallow clicks on the reroll button while the chest is worth keeping |
| `/kismet prices` | what it thinks each drop is worth, and where the price came from |
| `/kismet unknown` | reward lines it could not identify, and items with no price |

## The meter

The meter moves the threshold by roughly two million coins, so it is worth keeping honest. Open
the M7 RNG meter menu once and the mod reads your progress off it, then follows along by counting
the score of each run afterwards.

**Reopen that menu whenever the meter resets** — when the handle actually drops, for instance.
The mod only ever counts score upwards, so it cannot see a reset on its own and will keep
reporting a meter that is fuller than yours really is until you reseed it.

`/kismet meter <0-100>` sets the number by hand if you would rather not open the menu.

Settings live in `config/kismetcalc.properties`.

## Credits

The maths is Splooder's (`splooder` in game). Every threshold this mod prints comes from his
model; the mod only reads chests off the screen and does the arithmetic.
