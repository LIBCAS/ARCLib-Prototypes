**Database**
* user: **arclib**
* password: **vuji61oilo**
* urls:
  * **jdbc:postgresql://localhost:5432/arclib-1**
  * **jdbc:postgresql://localhost:5432/arclib-7**
  * **jdbc:postgresql://localhost:5432/arclib-8**
  * **jdbc:postgresql://localhost:5432/arclib-9**
  * **jdbc:postgresql://localhost:5432/arclib-10**
  * **jdbc:postgresql://localhost:5432/arclib-12**

Some prototypes need aditional configuration:

**prototype 3**

* your local antivirus should be deactivated otherwise it blocks **eicar** test file
* ClamAV antivirus has to be installed, after instalation:
  * create **database** and **quarantine** folders inside ClamAV folder
  * copy **freshclam.conf** from prototype 3 resources folder to ClamAV folder
  * run **freshclam.exe**
* **clamscan** command has to be added to PATH variable
* there must be **CLAMAV** environment variable pointing to CLAMAV directory

**prototype 6**

* path to the directory with the binary of DROID must be added to PATH