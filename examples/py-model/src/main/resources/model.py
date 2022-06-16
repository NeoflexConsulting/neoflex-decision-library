import mlflow
import pandas as pd
from pipe_wrapper import PipeWrapper

logged_model = '../../mlflow/examples/sklearn_elasticnet_wine/mlruns/0/b9defef6a9c8401a9ed9252fbec25d1e/artifacts/model'

loaded_model = mlflow.pyfunc.load_model(logged_model)


def predict(data):
    predictions = loaded_model.predict(pd.DataFrame([data]))
    return predictions[0]


PipeWrapper(predict).run()
