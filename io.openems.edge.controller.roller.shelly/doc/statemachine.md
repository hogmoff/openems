# State-Machine

```mermaid
graph LR
Undefined -->|target UP| RunningUp
Undefined -->|target DOWN| RunningDown

RunningDown -->|isRunning && !posReached| RunningDown
RunningDown -->|otherwise| Undefined
RunningDown -->|posReached && summerMode| Wait
RunningDown -->|posReached && !summerMode| RollerClosed

RunningUp -->|isRunning && !posReached| RunningUp
RunningUp -->|posReached| RollerOpen

RollerOpen -->|not timeout| RollerOpen
RollerOpen -->|otherwise| Undefined
RollerOpen -->|target UP| RunningUp
RollerOpen -->|target DOWN| RunningDown

Wait -->|waitingTimeOver| SlateToOpenPos
Wait -->|timeout| Undefined

SlateToOpenPos -->|always| RollerClosedWithOpenSlate

RollerClosed -->|not timeout| RollerClosed
RollerClosed -->|otherwise| Undefined
RollerClosed -->|target UP| RunningUp
RollerClosed -->|target DOWN| RunningDown

RollerClosedWithOpenSlate -->|not timeout| RollerClosedWithOpenSlate
RollerClosedWithOpenSlate -->|target UP| RunningUp
RollerClosedWithOpenSlate -->|target DOWN| RunningDown
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor