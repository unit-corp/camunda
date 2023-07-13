/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {
  unstable_HistoryRouter as HistoryRouter,
  Route,
  Routes,
} from 'react-router-dom';
import {ThemeProvider} from 'modules/theme/ThemeProvider';
import {NotificationProvider} from 'modules/notifications';
import {Notifications} from 'modules/carbonNotifications';
import {Login} from './Login';
import {Dashboard} from './Dashboard';
import {Processes} from './Processes';
import {ProcessInstance} from './ProcessInstance';
import {Decisions} from './Decisions';
import {DecisionInstance} from './DecisionInstance';
import GlobalStyles from './GlobalStyles';
import {NetworkStatusWatcher} from './NetworkStatusWatcher';
import {CommonUiContext} from 'modules/CommonUiContext';
import {Paths} from 'modules/routes';
import {CarbonPaths} from 'modules/carbonRoutes';
import {RedirectDeprecatedRoutes} from './RedirectDeprecatedRoutes';
import {AuthenticationCheck} from './AuthenticationCheck';
import {SessionWatcher} from './SessionWatcher';
import {Layout} from './Layout';
import {TrackPagination} from 'modules/tracking/TrackPagination';
import {useEffect} from 'react';
import {tracking} from 'modules/tracking';
import {currentTheme} from 'modules/stores/currentTheme';
import {createBrowserHistory} from 'history';
import {ThemeSwitcher} from 'modules/components/ThemeSwitcher';
import loadable from '@loadable/component';

const CarbonLogin = loadable(() => import('./Carbon/Login'), {
  resolveComponent: (components) => components.Login,
});

const CarbonLayout = loadable(() => import('./Carbon/Layout/index'), {
  resolveComponent: (components) => components.Layout,
});

const CarbonDashboard = loadable(() => import('./Carbon/Dashboard/index'), {
  resolveComponent: (components) => components.Dashboard,
});

const CarbonDecisions = loadable(() => import('./Carbon/Decisions/index'), {
  resolveComponent: (components) => components.Decisions,
});

const CarbonProcesses = loadable(() => import('./Carbon/Processes/index'), {
  resolveComponent: (components) => components.Processes,
});

const CarbonProcessInstance = loadable(
  () => import('./Carbon/ProcessInstance/index'),
  {
    resolveComponent: (components) => components.ProcessInstance,
  }
);

const CarbonDecisionInstance = loadable(
  () => import('./Carbon/DecisionInstance/index'),
  {
    resolveComponent: (components) => components.DecisionInstance,
  }
);

const App: React.FC = () => {
  useEffect(() => {
    tracking.track({
      eventName: 'operate-loaded',
      theme: currentTheme.state.selectedTheme,
    });
  }, []);

  return (
    <ThemeProvider>
      <ThemeSwitcher />
      <Notifications />
      <NotificationProvider>
        <NetworkStatusWatcher />
        <CommonUiContext />
        <HistoryRouter
          history={createBrowserHistory({window})}
          basename={window.clientConfig?.contextPath ?? '/'}
        >
          <RedirectDeprecatedRoutes />
          <SessionWatcher />
          <TrackPagination />
          <Routes>
            <Route
              path={Paths.login()}
              element={
                <>
                  <GlobalStyles />
                  <Login />
                </>
              }
            />
            <Route path={CarbonPaths.login()} element={<CarbonLogin />} />
            <Route
              path={Paths.dashboard()}
              element={
                <AuthenticationCheck redirectPath={Paths.login()}>
                  <GlobalStyles />
                  <Layout />
                </AuthenticationCheck>
              }
            >
              <Route index element={<Dashboard />} />
              <Route path={Paths.processes()} element={<Processes />} />
              <Route
                path={Paths.processInstance()}
                element={<ProcessInstance />}
              />
              <Route path={Paths.decisions()} element={<Decisions />} />
              <Route
                path={Paths.decisionInstance()}
                element={<DecisionInstance />}
              />
            </Route>
            <Route
              path={CarbonPaths.dashboard()}
              element={
                <AuthenticationCheck redirectPath={CarbonPaths.login()}>
                  <CarbonLayout />
                </AuthenticationCheck>
              }
            >
              <Route index element={<CarbonDashboard />} />
              <Route
                path={CarbonPaths.processes()}
                element={<CarbonProcesses />}
              />
              <Route
                path={CarbonPaths.processInstance()}
                element={<CarbonProcessInstance />}
              />
              <Route
                path={CarbonPaths.decisions()}
                element={<CarbonDecisions />}
              />
              <Route
                path={CarbonPaths.decisionInstance()}
                element={<CarbonDecisionInstance />}
              />
            </Route>
          </Routes>
        </HistoryRouter>
      </NotificationProvider>
    </ThemeProvider>
  );
};

export {App};
