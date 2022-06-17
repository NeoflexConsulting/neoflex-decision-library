# NDK Syntax by examples

The NDK syntax is a set of different operators. Each operator can contain the attributes `id` and `name`, where name is an optional attribute.
The `name` attribute is used for logging and flow rendering purposes. The `id` attribute is used to track flow execution and testing. 

For now there are available such operators:

- action
- rule
- gateway
- flow
- forEach
- while
- table
- python operator

To start creating your beautiful business flows you need some imports:
```scala
import ru.neoflex.ndk.dsl.syntax._
import ru.neoflex.ndk.dsl.implicits._
```
Let's start with an action.

### Action

```scala
action("sop-a-1", "Set output parameters") {
  chQuality.amountOfPayment = values.amountOfPayment
  chQuality.totalSumCRE = values.totalSumCRE
  chQuality.maxDelinquencyOverallHistory = applicant.externalCheck.loansOverview.worstStatusEver
  chQuality.ratioPastDueOverTotalAmount = values.totalDelqBalance / values.totalSumCRE
}
```
The above code is a simple example of an action declaration.
It also could be declared as a separate class with variables in the constructor that could be used in action's body:
```scala
case class ScoreAction(in: ScorePerson) extends ActionBase({
    in.score += 1
    println(s"ScorePerson after adding value: $in")
})
```
An `Action` is an operator, that can be executed without any conditions. Actions require specifying only it's body.

### Rule

```scala
rule("rule-id", "Simple rule name") {
  condition("Condition name", 1 == 1) andThen {
    println("True condition action")
  } condition("Another condition", 2 == 2) andThen {
    println("Another condition action")
  } otherwise {
    println("False condition action")
  }
}
```
The code above is a simple example of a rule declaration. `Rule` must contain at least one condition with action.
It is also possible to specify additional conditions. Of all the true conditions only one branch will be executed.
The `otherwise` branch will be executed if all other branch conditions was false. The `otherwise` branch is optional. 

### Flow

Flow — is a special operator that can contain a sequence of many other operators, including other flows.
In other words, a flow is a business process consisting of many blocks that are processed sequentially.
User defined flow class should be extended from the abstract class `ru.neoflex.ndk.dsl.Flow`. As in a user
defined action class, variables also could be defined in the constructor and further used in the body of the flow.
Each operator that has been defined in the flow will be executed in the order in which they are specified.

```scala
case class ScoringFlow(in: Person, out: ApplicationResponse)
    extends Flow(
      "psc-f-1",
      "Person scoring calculation flow",
      flowOps(
        rule("s-r-1") {
          condition("sex = WOMAN", in.sex == "WOMAN") andThen {
            out.scoring += 50
          } otherwise {
            out.scoring -= 10
          }
        },

        rule("a-r-1") {
          condition("age < 39", in.age < 39) andThen {
            out.scoring += 13
          } condition("age >= 30 and age <= 39", in.age >= 30 && in.age <= 39) andThen {
            out.scoring += 17
          } condition("age >= 40 and age <= 49", in.age >= 40 && in.age <= 49) andThen {
            out.scoring += 23
          } otherwise {
            out.scoring += 23
          }
        },

        action("a-r-2", "Finished") {
          println(s"Scoring flow finished. Scoring value: ${out.scoring}")
        }
      )
    )
```

### Gateway

Gateway — is a special operator that can change direction of flow execution depending on the conditions defined by the user.
Gateway consists of several conditions and the otherwise branch. Each condition can have a name that will be displayed in the visualization of the flow.
After the condition there must be a body that is any another operator.
Only the first branch whose condition is true will be executed or the otherwise branch if no conditions has been met.

```scala
gateway("rl-g-1", "Risk level gateway") {
  when("Level 1 or 3") { out.riskLevel != 2 } andThen {
    rule("srl-r-1") {
      condition("riskLevel = 1", out.riskLevel == 1) andThen {
        out.underwritingRequired = false
      } otherwise {
        out.underwritingRequired = true
        out.underwritingLevel = 2
      }
    }
  } otherwise flow("us-f-1", "Underwriting by scoring value")(
    ScoringFlow(applicant.person, out),

    rule("uwl-r-1") {
      condition("scoring < 150?", out.scoring < 150) andThen {
        out.underwritingRequired = true
        out.underwritingLevel = 2
      } otherwise {
        out.underwritingRequired = true
        out.underwritingLevel = 1
      }
    }
  )
}
```

### ForEach

ForEach is a loop operator over collections. It can be declared as an operator containing another operator, or
as an operator containing simple action with the next collection element.

Example of the flow that contain a `for each` loop on a collection of loans:

```scala
case class BadDebtCalculating(loans: Seq[Loan], chQuality: CreditHistoryQuality)
  extends Flow(
    "bdc-f-1",
    Some("Bad debt calculation"),
    forEachOp("bdc-fel-1", Some("Has more loans to determine bad debt existence?"), loans) { loan =>
      flow(
        forEachOp("Payment discipline", Some("More values of payment discipline?"), loan.paymentDiscipline) { mannerOfPayment =>
          rule("manner of payment") {
            condition(Some("Is manner of payment any of 5, 7, 8, 9?"), "5789" contains mannerOfPayment) andThen {
              chQuality.isBadDebtCRE = 'Y'
            }
          }
        },
        rule("loan state") {
          condition(Some("loanState > 21?"), loan.loanState > 21) andThen {
            chQuality.isBadDebtCRE = 'Y'
          }
        }
      )
    }
  )
```

Example of the simple action in the loop declaration:

```scala
forEach(NoId, "Has more values for precalculation?", loans) { loan =>
  println(loan)
}
```

### While

While is a loop with a condition that is checked every time before executing the loop body. Where the body of the loop is another operator.

Example of simple loop that will never complete:

```scala
whileLoop("wl-1", Some("Simple while loop"), 1 == 1) {
  action {
    println("It's forever")
  }
}
```

### Table

Table is an operator that implements decision table construction.
The table consists of two sections, the first is the expressions section and the second is the conditions section.
Each row in conditions section declares a condition set according to declared early expressions. Each row
must contain the exact number of conditions and in the order corresponding to expressions in the expressions section.
Each row all conditions of which are met will be executed.

Below is an example of the decision table with two expressions:

```scala
case class RiskLevelTable(in: Applicant, out: ApplicationResponse)
    extends Table(
      "rl-t-1",
      "Risk level table",
      expressions (
        "role" expr in.role,
        "channel" expr in.channel
      ) andConditions (
        row(eqv("APPLICANT"), eqv("RECOMMEND")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("APPLICANT"), eqv("PROMO")).apply("riskLevel = 3") { out.riskLevel = 3 },
        row(eqv("APPLICANT"), eqv("STREET")).apply("riskLevel = 3") { out.riskLevel = 3 },
        row(eqv("LOAN1"), eqv("RECOMMEND")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN1"), eqv("PROMO")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN1"), eqv("STREET")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("RECOMMEND")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN2"), eqv("PROMO")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN2"), eqv("STREET")).apply("riskLevel = 2") { out.riskLevel = 2 },
        row(eqv("LOAN3"), eqv("RECOMMEND")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("PROMO")).apply("riskLevel = 1") { out.riskLevel = 1 },
        row(eqv("LOAN3"), eqv("STREET")).apply("riskLevel = 1") { out.riskLevel = 1 },
      )
    )
```

`"role" expr in.role` declares expression `in.role` with the name `role`. The name will be used in the visualization.
Expression will be evaluated only once and then will be used in the condition of each row.
Expression can also look like `in.role` without the name. 

The syntax of the row declaration is as follows: `row(${condition operators set}).apply(${optional row name}) { ${body} }`
where:
- `${condition operators set}` — comparing operator with the value for each expression, a list of available operators:
  - eqv
  - neq
  - gt
  - lt
  - gte
  - lte
  - empty
  - nonEmpty
  - any
- `${optional row name}` — name of the row which will be displayed in the visualization tool if specified
- `${body}` — body of the row which will be executed when conditions of the row are met. The body may be declared in two different forms:
  - as simple action in curly braces
  - or as call to early defined action { ${action name} withArgs ${action arguments} }

Below is another example of the table with early declaration of the action:

```scala
case class PrintConditionsTable(in: Applicant, out: ApplicationResponse)
    extends Table(
      "pc-t-1",
      "Print conditions",
      expressions (
        "role" expr in.role,
        "channel" expr in.channel
      ) withActions (
        "print" action { (role, channel) =>
          println(s"role is: $role, channel is: $channel")
        }
      ) andConditions (
        row(eqv("APPLICANT"), eqv("RECOMMEND")).apply { "print" withArgs(in.role, in.channel) }
        row(eqv("APPLICANT"), eqv("PROM")).apply { "print" withArgs(in.role, in.channel) }
        row(eqv("LOAN3"), eqv("STREET")).apply { "print" withArgs(in.role, in.channel) }
      )
    )
```

### Python operator

`PythonOperator` — is a special operator which helps with calling scripts written in python from a flow.
Interaction between java and python processes performs via sending json structures via pipe(stdin). Step by step interaction looks as follows:

- serialize user data structure to json string
- send json string to python process via pipe
- receive json string like response from python process
- deserialize string to user data structure
- call user response handler with deserialized structure

Bellow is an example of the python operator declaration:
```scala
final case class WineModelFlow(data: WineModelData)
    extends Flow(
      "wm-f-1",
      "WineModel",
      flowOps(
        pythonCall[Seq[Double], Double](
          "pm-c-1",
          "Call wine model",
          "examples/py-model/src/main/resources/model.py",
          data.features
        ) { result =>
          data.result = result
        }
      )
    )
```

`pythonCall[Seq[Double], Double]` — here in square brackets specified two type parameters:

1. First type parameter is a type of input data structure for the python process. Here it is `Seq[Double]` — sequence of doubles(array in other words)
2. Second type parameter is a type of output data structure returned from the python process. Here it is just number with double precision.

As the type parameter there could be any type that the user wants to use or has created, but there must be `io.circe.Encoder` and `io.circe.Decoder`
for input and output respectively. Fortunately, for basic scala types there already exist encoders and decoders,
for custom types you need to create a new one.

`examples/py-model/src/main/resources/model.py` — this is the location of the python script to be called.

`data.features` — this is the input data for the python script.

`{ result =>
  data.result = result
}` — this is the result handler.

The above example is only scala side of the interaction, there is also python side.
Bellow is an example of the `model.py` script:
```python
import mlflow
import pandas as pd
from pipe_json_wrapper import PipeJsonWrapper

logged_model = '../../mlflow/examples/sklearn_elasticnet_wine/mlruns/0/b9defef6a9c8401a9ed9252fbec25d1e/artifacts/model'
loaded_model = mlflow.pyfunc.load_model(logged_model)


def predict(data):
    predictions = loaded_model.predict(pd.DataFrame([data]))
    return predictions[0]


PipeJsonWrapper(predict).run()

```

`predict` is your function which will be called with deserialized from json data come from the java process.
Data returned by the `predict` function will be automatically serialized to json and returned to the java process.

Also, to run your function with the pipe interface you need the following wrapper class:
```python
import json
import sys


def identity(x):
    return x


class PipeJsonWrapper:
    def __init__(self, user_fn, map_input=identity, map_output=identity):
        self.user_fn = user_fn
        self.map_input = map_input
        self.map_output = map_output

    def run(self):
        for l in iter(sys.stdin.readline, ''):
            line = l.strip()
            input_data = json.loads(line)
            result = self.user_fn(input_data)
            result = self.map_output(result)
            output_data = json.dumps(result)
            print(output_data)
            sys.stdout.flush()
```

This class listens to stdin, deserializes input data, calls user function, serializes the response and sends it to stdout.

## Testing

TBD
