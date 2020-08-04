### Build
Creates a fatjar in build/libs.

``gradle jar``

### Run
Assumes AWS IAM credentials are available via IAM roles. Refer [run_with_aws_creds script](run_with_aws_creds.sh).
``java -jar neptune-bulk-drop-1.0-SNAPSHOT.jar <neptune cluster endpoint> <file with ids to drop> <E/V> <num id to drop at once> <num worker threads>``
