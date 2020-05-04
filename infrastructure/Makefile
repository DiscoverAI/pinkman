stack_name = "SARS-CoV-2"

.PHONY: infrastructure
infrastructure:
	aws cloudformation deploy --template-file datalake.yaml --stack-name $(stack_name)