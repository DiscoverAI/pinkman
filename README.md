# Pinkman
> Yeah Mr. White! You really do have a plan! Yeah science!
>
> -- Jesse Bruce Pinkman (Breaking Bad)

A _Scala_ job that takes the [moses dataset](https://github.com/molecularsets/moses) and splits it into `test.parquet` and `train.parquet` files.

It uses a s3 bucket for storing datasets.

## Configuration
Please set the following environment variables accordingly:
```dotenv
AWS_ACCESS_KEY_ID="<aws access key id>"
AWS_SECRET_ACCESS_KEY="<aws secret access key>"
S3_BUCKET_NAME="<s3 bucket name that contains the moses dataset>"
MLFLOW_TRACKING_URI="<url of the ml flow tracking server>"
```

## Input
It assumes that there is a moses.csv file lying in the root of your container of your azure storage.

## Output
It produces `train.parquet` `test.parquet` files inside a `pinkman` folder on the specified s3 bucket.
