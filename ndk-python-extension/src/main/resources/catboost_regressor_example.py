import polyglot
import mlflow


class CatBoostExampleModel:
    def __init__(self):
        self.model = mlflow.catboost.load_model('mlruns/0/9debfff327c14dbc82dc9b204eeda19c/artifacts/model')

    def predict(self, input):
        return self.model.predict(input)


polyglot.export_value('CatBoostExampleModel', CatBoostExampleModel)
