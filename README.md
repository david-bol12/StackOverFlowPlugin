## Inspiration

As young software engineers JetBrains IDE's are our go to as they provide all the necessary tools to make developing software a friction-less and more enjoyable experience. This made the JetBrains track an obvious choice for our team.

On top of this, AI dependence is becoming more and more prevalent in the SWE world. While this has it's obvious benefits it has a negative effect on education, with AI for many young developers becoming a crutch instead of a tool.

To combat this we want to make debugging far less AI dependant by integrating StackOverFlow into the IDE. This helps StackOverFlow replace LLMs as the default debugging resource.

## What it does

- Automatically obtains the top 3 answers from StackOverFlow API and displays them in an IDE window.
- Adds an option to view the StackOverFlow site on a browser within the IDE.
- Allows the user to cycle between multiple errors.
- Highlights the line where the error is located.

## How we built it

## Challenges we ran into
The biggest challenge we faced was defintily ideating. What we struggled with most was deciding on how to build upon what we found to be almost a perfect IDE. We brainstormed a multitude of ideas but realised they have either been done before or didn't add anything meaningful to the IDE. After deciding on our solution, we encountered even more challenges. Firstly, no one in our team had ever used Kotlin or the JetBrain's plugin API before and thus we had to learn it from scratch. Secondly, given it was most of out team's first big hackathon, out time managment left a lot to be desired, on reflection we realised that we spent too much time ideating and not enough time innovating and building the solution. Finally, the biggest challenge we faced from a techincal aspect was getiing out plugin to work across the multitude of JetBrain IDEs. Unfourtantly we were unable to get it working to it's full potential in PyCharm and CLion, but after quite a bit of problem solving we still built a viable solution.

## Accomplishments that we're proud of

## What we learned

[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:ipe]: https://jb.gg/ipe

[jb:ui-guidelines]: https://jetbrains.github.io/ui
