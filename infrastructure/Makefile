project_name = "sars-cov-2"
bucket_name = "$(project_name)-25309b4013524"
user_name = "datalake-ci-deploy"
region = "eu-central-1"
developer_profile = "tortugas-developer"

now := $(shell date +%s)
wait_for_status_change=5

.PHONY: deploy-user-key-template-test
deploy-user-key-template-test:
	aws cloudformation validate-template --template-body file://./deploy-user-key.yaml >> /dev/null

.PHONY: deploy-user-key
deploy-user-key:
	aws cloudformation deploy \
		--template-file deploy-user-key.yaml \
		--stack-name "$(project_name)-deploy-user-key" \
		--capabilities CAPABILITY_NAMED_IAM \
		--parameter-overrides UserName=$(user_name)
	aws cloudformation describe-stacks \
		--stack-name "$(project_name)-deploy-user-key" \
		--query "Stacks[0].Outputs[*].OutputValue" | jq

.PHONY: deploy-user-template-test
deploy-user-template-test:
	aws cloudformation validate-template --template-body file://./deploy-user.yaml >> /dev/null

.PHONY: deploy-user
deploy-user:
	aws cloudformation deploy \
		--template-file deploy-user.yaml \
		--stack-name "$(project_name)-deploy-user" \
		--capabilities CAPABILITY_IAM \
		--parameter-overrides ProjectName=$(project_name) DatalakeStackName="$(project_name)-datalake" UserName=$(user_name)

.PHONY: datalake-template-test
datalake-template-test:
	aws cloudformation validate-template --template-body file://./datalake.yaml >> /dev/null

.PHONY: datalake
datalake:
	aws cloudformation deploy \
		--template-file datalake.yaml \
		--stack-name "$(project_name)-datalake" \
		--parameter-overrides BucketName=$(bucket_name) ProjectName=$(project_name)

.PHONY: update-datalake
update-datalake:
	aws cloudformation create-change-set \
		--change-set-name "$(project_name)-datalake-${now}" \
		--change-set-type UPDATE \
		--template-body file://./datalake.yaml \
		--stack-name "$(project_name)-datalake" \
		--region $(region) \
		--capabilities CAPABILITY_NAMED_IAM \
		--profile $(developer_profile) \
		--parameters ParameterKey=ProjectName,ParameterValue=$(project_name) ParameterKey=BucketName,ParameterValue="$(bucket_name)"

	sleep $(wait_for_status_change)

	aws cloudformation execute-change-set \
		--change-set-name "$(project_name)-datalake-${now}" \
		--stack-name "$(project_name)-datalake" \
		--region $(region) \
		--profile $(developer_profile)

.PHONY: templates-test
templates-test: datalake-template-test deploy-user-template-test deploy-user-key-template-test

.PHONY: infrastructure
infrastructure: datalake deploy-user deploy-user-key
