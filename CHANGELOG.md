
## next

yyyy-mm-dd

- Fixed alliance check during login.
- Added button to set the start system back to the current location.
- Added button to swap start and end systems. (thanks [@NevarrTivianne](https://github.com/NevarrTivianne))
- Added number of jumps to route list.
- Small layout improvements.
- Map: Added title (on mouse over) to systems.
- Map: Connections are redrawn when a temporary connection has been added or removed.
- Added CSRF protection for all requests that change data.
- Added option to enable the "secure" flag for the session cookie.
- More efficient storage of Ansiblex gates in the database.
- Added database migrations.
- Some cleanups, library updates and improved documentation.

## 0.2.0

2020-09-20

- [#3][i3] Added support for PostgreSQL, MySQL, MariaDB, SQLite and H2 (embedded mode) databases.
- User is logged off from the frontend if the backend responds with a "Forbidden" error.
- Fixed some MongoDB operations.

[i3]: https://github.com/tkhamez/eve-route/issues/3

## 0.1.0

2020-09-19

First release.
