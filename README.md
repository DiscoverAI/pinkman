# Pinkman
[![CircleCI](https://circleci.com/gh/DiscoverAI/pinkman.svg?style=shield)](https://circleci.com/gh/DiscoverAI/pinkman)
[![GitHub license](https://img.shields.io/github/license/DiscoverAI/pinkman)](https://github.com/DiscoverAI/pinkman/blob/master/LICENSE)
[![Gitter](https://badges.gitter.im/discoverai/community.svg)](https://gitter.im/discoverai/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

> Yeah Mr. White! You really do have a plan! Yeah science!
>
> -- Jesse Bruce Pinkman (Breaking Bad)

A _Scala Spark_ job that takes the [moses dataset](https://github.com/molecularsets/moses) and splits it into `test.csv`
and `train.csv` and creates a `dictionary.csv` containing the substitution rules for each character.

It uses a s3 bucket for storing datasets.

## Get Submodules
```bash
git submodule update --init --recursive
git pull --recurse-submodules && git submodule update --remote
```

## Test
```bash
./gradlew check
```

## Build
To create a jar in `./build/libs/pinkman-all.jar`:
```bash
./gradlew shadowJar
```

## Run
When running the jar please make sure to provide the s3 bucket that contains the input file (first parameter) and the
url to your mlflow instance as second parameter, e.g.:
```bash
./bin/spark-submit \
  --class com.github.discoverai.pinkman.Pinkman \
  --master <master-url> \
  --deploy-mode <deploy-mode> \
  <jar-uri> \
  <datalake-bucket> <mlflow-tracking-server-uri>
```
Where
```
<master-url> = Your Spark cluster master uri
<deploy-mode> = Your deploy mode on your Spark cluster
<jar-uri> = URI to the jar, e.g.: s3://bucket/pinkman-all.jar
<mlflow-tracking-server-uri> = Accessible URI to your mlflow instance, e.g.: http://mlflow-tracking.server
```

## Input
It assumes that there is a moses.csv file lying in the root of your provided datalake bucket.



### Example
moses.csv
```csv
SMILES,SPLIT
CCCS(=O)c1ccc2[nH]c(=NC(=O)OC)[nH]c2c1,train
CC(C)(C)C(=O)C(Oc1ccc(Cl)cc1)n1ccnc1,train
CC1C2CCC(C2)C1CN(CCO)C(=O)c1ccc(Cl)cc1,test
...
```

## Output
It produces `train.csv`, `test.csv`, `dictionary.csv` folders with files inside a `pinkman` folder on the specified
s3 bucket. The dictionary contains the substitution rules for each character into a number (word embedding).

### Example
(train.csv and test.csv)/part-xxxxxxx.csv:
```csv
25.0,25.0,23.0,...,11.0
```
Please note that the length of each row is not fixed.


dictionary.csv/part-xxxxxxx.csv:
```csv
6,16,1.0
5,2246,2.0
r,58572,3.0
B,58572,4.0
4,98530,5.0
```
where the schema is:
```csv
original string, number of appearances, word embedding (substituted number)
```
The dictionary is ordered by number of appearances. The string that appears the least in the dataset has the embedding
`1.0`. The string that appears the most has the highest index (embedding). The embeddings are integers formatted as
rational number. `0.0` is reserved for paddings (filling out non-existent entries in the vector to keep stable dimensions).
