#!/usr/bin/env bash

role_arn=arn:aws:iam::160071257600:role/federated-admin

STS=$(aws sts assume-role --role-arn $role_arn --role-session-name aws-cli-session --profile default)
if [ $? -eq 0 ]; then
    export AWS_ACCESS_KEY_ID=$(echo $STS | jq -r .Credentials.AccessKeyId)
    export AWS_SECRET_ACCESS_KEY=$(echo $STS | jq -r .Credentials.SecretAccessKey)
    export AWS_SESSION_TOKEN=$(echo $STS | jq -r .Credentials.SessionToken)
else
  echo "Something went wrong. Could not assume role."
fi