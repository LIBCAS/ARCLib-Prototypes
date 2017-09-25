Some prototypes need aditional configuration:

**prototype 3**
* your local antivirus should be deactivated otherwise it blocks **eicar** test file
* ClamAV antivirus has to be installed, after instalation:
  * create **database** and **quarantine** folders inside ClamAV folder
  * copy **freshclam.conf** to ClamAV folder
  * run **freshclam.exe**
* **clamscan** command has to be added to PATH variable
* there must be **CLAMAV** environment variable pointing to CLAMAV directory