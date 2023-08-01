# PTMK
## general information
To run the app you need to have JRE 8 (java 8) installed, as well as the 'MySQL' DBMS.
### Tasks
1. For creating table you should set up following settings for DBMS:
   - login: PTMK_admin;
   - password: PTMK@1admin.

*Otherwise, the app will prompt you to enter data manually*

2. For creating user you should use the following format:
  - first name;
  - patronymic [not necessary];
  - birthday [YYYY-MM-DD];
  - sex [{1, мужской, male, M} - for man; {2, женский, female, f} - for woman].

3. The query can be delayed by extremelly unique rows and no indexes, because for generating full name was used the files with a lot of names and surnames.
4. Initially, after user generating the app tries to submit through the file, but if there are no permissions to make changes to the DBMS, it sends the "data-batch" directly.
5. here it's obvious.
6. To speed up the query, prefix index with one letter were used (because the query only uses the first letter in surname for search). Adding an index for the 'sex' field would only slow down the query, as this field contains non-unique and frequently repeating values (1, 2). Screenshot is presented with result of indexation (you can try by yourself use java -jar ... 6 after filling table)

[screenshot](https://github.com/ivan8661/PTMK/blob/master/res.png) 
