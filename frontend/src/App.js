import $ from 'jquery';
import React from 'react';
import './App.css';

class App extends React.Component {
  render() {
    return (
      <div className="App">
        <h1>EVE Route</h1>

        <div id="login" className="cloak">
          <a href={this.domain+'/api/auth/login'}><img
            src="https://web.ccpgamescdn.com/eveonlineassets/developers/eve-sso-login-black-small.png"
            alt="login"/></a>
        </div>

        <div id="home" className="cloak">
          <p>
            Hello <span id="homeUser"> </span><br/>
            <a href={this.domain+'/api/auth/logout'}>Logout</a>
          </p>
          <p id="route">
            <label>from <input type="text" name="from"/></label>
            <label>to <input type="text" name="to"/></label>
            <button onClick={this.routeCalculate}>calculate</button>
            <button onClick={this.routeSet}>set route</button>
            <span id="routeSetResult"> </span><br/>
            <a href="#" target="_blank">Dotlan</a><br/>
            <span id="routeCalculateResult"> </span><br/>
          </p>
          <p id="gates">
            <button onClick={this.gatesFetch}>show gates</button>
            <button onClick={this.gatesUpdate}>update gates</button>
            last update: <span id="gatesUpdated"> </span><br/>
            <span id="gatesResult"> </span><br/>
          </p>
        </div>
      </div>
    );
  }

  constructor(props) {
    super(props);

    this.domain = '';
    if (window.location.port === '3000') {
      this.domain = 'http://localhost:8080'; // backend dev port
    }

    this.gatesUpdated = this.gatesUpdated.bind(this);
    this.gatesFetch = this.gatesFetch.bind(this);
    this.gatesUpdate = this.gatesUpdate.bind(this);
    this.routeCalculate = this.routeCalculate.bind(this);
    this.routeSet = this.routeSet.bind(this);

    $.ajaxSetup({crossDomain: true, xhrFields: { withCredentials: true } });

    $.get(this.domain+'/api/auth/user')
      .done(function(data) {
        $('#home').show();
        $('#homeUser').text(data.characterName + ' ' + (data.allianceId || '(unknown alliance)'));
      })
      .fail(function() { // 403
        $('#login').show();
      });

    this.gatesUpdated();

    this.esiRoute = []
  }

  gatesUpdated() {
    $.get(this.domain+'/api/gates/last-update')
      .done(function(data) {
        if (data) {
          $('#gatesUpdated').text(data.updated);
        }
      })
  }

  gatesFetch(event) {
    const button = event.target;
    button.disabled = true;
    $('#gatesResult').html('');
    $.get(this.domain+'/api/gates/fetch')
      .done(function(data) {
        let gates = '';
        for (let i = 0; i < data.ansiblexes.length; i++) {
          gates += data.ansiblexes[i].name + '<br>';
        }
        $('#gatesResult').html(gates);
      })
      .fail(function() {
        $('#gatesResult').text('Error.');
      })
      .always(function() {
        button.disabled = false;
      });
  }

  gatesUpdate(event) {
    const button = event.target;
    button.disabled = true;
    const $result = $('#gatesResult');
    $result.html('');
    const app = this
    $.get(this.domain+'/api/gates/update')
      .done(function(data) {
        if (data.message) { // some error
          $result.text(data.message);
          return;
        }
        let gates = '';
        for (let i = 0; i < data.ansiblexes.length; i++) {
          gates += data.ansiblexes[i].name + '<br>';
        }
        $result.html(gates);
        app.gatesUpdated();
      })
      .fail(function() {
        $result.text('Error.');
      })
      .always(function() {
        button.disabled = false;
      });
  }

  routeCalculate(event) {
    const button = event.target;
    button.disabled = true;
    $('#routeCalculateResult').text('');
    const from = $('#route input[name="from"]').val();
    const to = $('#route input[name="to"]').val();
    $.get(this.domain+'/api/route/calculate/' + from + '/' + to)
      .done(function(data) {
        if (data.route.length === 0) {
          $('#routeCalculateResult').text('No route found.');
        } else {
          this.esiRoute = []
          let route = '';
          let dotlanHref = 'https://evemaps.dotlan.net/route/';
          for (let i = 0; i < data.route.length; i++) {
            this.esiRoute.push({
              systemId: data.route[i].systemId,
              systemName: data.route[i].systemName, // for debugging
              ansiblexId: data.route[i].ansiblexId || null,
            });
            route += data.route[i].systemName + ' ' + data.route[i].systemSecurity;
            if (data.route[i].ansiblexName) {
              route += ' "' + data.route[i].ansiblexName + '"';
            }
            route += '<br>';
            dotlanHref += data.route[i].systemName.replace(' ', '_')
            if (data.route[i].connectionType === "Stargate") {
              dotlanHref += ':'
            } else if (data.route[i].connectionType === "Ansiblex") {
              dotlanHref += '::'
            } // else = end system
          }
          $('#routeCalculateResult').html(route);
          $('#route a').attr('href', dotlanHref);
        }
      })
      .fail(function() {
        $('#routeCalculateResult').text('Error.');
      })
      .always(function() {
        button.disabled = false;
      });
  }

  routeSet(event) {
    const button = event.target;
    button.disabled = true;
    $('#routeSetResult').text("");
    $.post(this.domain+'/api/route/set', JSON.stringify(this.esiRoute))
      .done(function(result) {
        $('#routeSetResult').text(result.message);
      })
      .fail(function() {
        $('#routeSetResult').text('Error.');
      })
      .always(function() {
        button.disabled = false;
      });
  }
}

export default App;
