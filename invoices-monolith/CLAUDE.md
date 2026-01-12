Arch=Hexagonal
Layers=Presentation,Application,Domain,Infrastructure
Rule=Domain_no_import_Infrastructure

UseCases=1_service_per_operation
Tx=@Transactional_in_UseCase
Validation=@Valid_in_Controller
DTOs=Request_Response_only
Entities=never_exposed

Exceptions=Domain_specific
Logs=INFO_ops,DEBUG_detail,ERROR_fail

CleanCode=
- methods<30
- classes<300
- indent<=3
- early_return
- descriptive_names

Principles=KISS,YAGNI,DRY
