project_name = "sars-cov-2"
bucket_name = $(project_name)"-25309b4013524"

.PHONY: test-datalake-template
test-datalake-template:
	aws cloudformation validate-template --template-body file://./datalake.yaml >> /dev/null

.PHONY: datalake
datalake:
	aws cloudformation deploy \
		--template-file datalake.yaml \
		--stack-name "$(project_name)-datalake" \
		--parameter-overrides BucketName=$(bucket_name) ProjectName=$(project_name)

.PHONY: test-templates
test-templates: test-datalake-template

.PHONY: infrastructure
infrastructure: datalake
