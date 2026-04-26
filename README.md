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
The project is coded in Kotlin and built using Gradle. We make use of the provided JetBrain's Plugin API to build our solution. This offered many helpful Classes and Functions. Under the hood, the plugin opens a socket which calls upon the StackOverFlow API. Text is essiently copy and pasted into a search box and the top three results are presented. The user can then choose to open a browser window in the IDE which brings them to the StackOverFlow search page of the error.

## Challenges we ran into
The biggest challenge we faced was defintily ideating. What we struggled with most was deciding on how to build upon what we found to be almost a perfect IDE. We brainstormed a multitude of ideas but realised they have either been done before or didn't add anything meaningful to the IDE. After deciding on our solution, we encountered even more challenges. Firstly, no one in our team had ever used Kotlin or the JetBrain's plugin API before and thus we had to learn it from scratch. Secondly, given it was most of out team's first big hackathon, out time managment left a lot to be desired, on reflection we realised that we spent too much time ideating and not enough time innovating and building the solution. Finally, the biggest challenge we faced from a techincal aspect was getiing out plugin to work across the multitude of JetBrain IDEs. Unfourtantly we were unable to get it working to it's full potential in PyCharm and CLion, but after quite a bit of problem solving we still built a viable solution.

## Accomplishments that we're proud of
Our team is extremly proud of our solution, we think it is a viable solution that has a definitve use case. It is sleak and user friendly and given the fact we had no prior experience with both Kotlin and JetBrain's API, we are very proud of the final product. We are proud of our work ethic and ability to learn which we feel we showed througout this expierence.

## What we learned
We found the hackathon to be a very valuable expierence and learned a lot throughout our time. Out biggest take away was time managment. Reflecting on our expierence we can see where we mismanaged out time and how we could have improved. Other things we learned were communication and effect ideation skills. Throughout the expierence our communcation skills improved and our disagreements were solved more and more diplomatically each time. We learned effective programming skills and a new languange Kotlin. We learned to identify a gap in a market and capatlise on it, along with a whole host of other skills. Overall we're very greatful for our expierence in HackUPC.

[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:ipe]: https://jb.gg/ipe

[jb:ui-guidelines]: https://jetbrains.github.io/ui
