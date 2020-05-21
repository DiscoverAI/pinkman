project_name = "sars-cov-2"
bucket_name = "$(project_name)-25309b4013524"
user_name = "datalake-ci-deploy"

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

.PHONY: templates-test
templates-test: datalake-template-test deploy-user-template-test deploy-user-key-template-test

.PHONY: infrastructure
infrastructure: datalake deploy-user deploy-user-key
