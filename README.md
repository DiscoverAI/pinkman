# Pinkman
> Yeah Mr. White! You really do have a plan! Yeah science!
>
> -- Jesse Bruce Pinkman (Breaking Bad)

A _Scala_ job that takes the [moses dataset](https://github.com/molecularsets/moses) and splits it into `test.csv` and `train.csv` files.

It uses a [azure blob storage](https://docs.microsoft.com/en-us/azure/storage/blobs/storage-blobs-introduction) for storing datasets.

## Configuration
Please set the following environment variables accordingly:
```dotenv
AZURE_STORAGE_ACCOUNT_NAME="<azure storage account name>"
AZURE_STORAGE_KEY="<azure storage key>"
AZURE_STORAGE_CONTAINER_NAME="<azure storage container name>"
```

## Input
It assumes that there is a moses.csv file lying in the root of your container of your azure storage.

## Output
It produces `train.csv` `test.csv` files locally
