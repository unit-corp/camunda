/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {ComponentProps, useEffect, useState} from 'react';
import {Link, matchPath, useLocation} from 'react-router-dom';
import {
  C3Navigation,
  C3UserConfigurationProvider,
  C3NavigationProps,
  C3NavigationElementProps,
  C3NavigationNavBarProps,
} from '@camunda/camunda-composite-components';

// @ts-ignore
import {NavItem} from 'components';
import {showError} from 'notifications';
import {t} from 'translation';
import {track} from 'tracking';
import {useDocs, useErrorHandling, useUiConfig} from 'hooks';

import {getUserToken} from './service';
import useUserMenu from './useUserMenu';

import './Header.scss';

const orderedApps = ['console', 'modeler', 'tasklist', 'operate', 'optimize'];

export default function Header({noActions}: {noActions?: boolean}) {
  const [userToken, setUserToken] = useState<string | null>(null);
  const location = useLocation();
  const {mightFail} = useErrorHandling();
  const {getBaseDocsUrl} = useDocs();
  const {
    optimizeProfile,
    enterpriseMode,
    webappsLinks,
    optimizeDatabase,
    optimizeVersion,
    onboarding,
    notificationsUrl,
    validLicense,
    licenseType,
  } = useUiConfig();
  const timezoneInfo =
    t('footer.timezone') + ' ' + Intl.DateTimeFormat().resolvedOptions().timeZone;
  const userSideBar = useUserMenu(optimizeVersion, timezoneInfo);

  useEffect(() => {
    mightFail(getUserToken(), setUserToken, showError);
  }, [mightFail]);

  const props: C3NavigationProps = {
    app: createAppProps(location),
    appBar: createAppBarProps(webappsLinks),
    navbar: {elements: []},
  };

  if (!noActions) {
    props.navbar = createNavBarProps(
      validLicense,
      licenseType,
      location.pathname,
      optimizeDatabase
    );
    props.infoSideBar = createInfoSideBarProps(getBaseDocsUrl(), enterpriseMode);
    props.userSideBar = userSideBar;
  }

  const isCloud = optimizeProfile === 'cloud';
  if (isCloud) {
    props.notificationSideBar = {
      isOpen: false,
      ariaLabel: 'Notifications',
    };
  }

  return (
    <NavbarWrapper
      isCloud={isCloud}
      notificationsUrl={notificationsUrl}
      userToken={userToken}
      getNewUserToken={getUserToken}
      organizationId={onboarding.orgId}
    >
      <C3Navigation {...props} />
    </NavbarWrapper>
  );
}

function createAppProps(location: {pathname: string}): C3NavigationProps['app'] {
  return {
    name: t('appName').toString(),
    ariaLabel: t('appFullName').toString(),
    routeProps: {
      as: Link,
      className: 'cds--header__name',
      to: '/',
      replace: location.pathname === '/',
    },
  };
}

function createAppBarProps(
  webappLinks: Record<string, string> | null
): C3NavigationProps['appBar'] {
  return {
    ariaLabel: t('navigation.appSwitcher').toString(),
    isOpen: false,
    elements: createWebappLinks(webappLinks),
    elementClicked: (app) => {
      track(app + ':open');
    },
  };
}

function createWebappLinks(webappLinks: Record<string, string> | null): C3NavigationElementProps[] {
  if (!webappLinks) {
    return [];
  }

  return orderedApps
    .filter((key) => webappLinks[key])
    .map<C3NavigationElementProps>((key) => ({
      key,
      label: t(`navigation.apps.${key}`).toString(),
      ariaLabel: t(`navigation.apps.${key}`).toString(),
      href: webappLinks[key],
      active: key === 'optimize',
      routeProps: key === 'optimize' ? {to: '/'} : undefined,
    }));
}

function createNavBarProps(
  validLicense: boolean,
  licenseType: 'production' | 'saas' | 'unknown',
  pathname: string,
  optimizeDatabase?: string
): C3NavigationNavBarProps {
  const elements: C3NavigationNavBarProps['elements'] = [
    {
      key: 'dashboards',
      label: t('navigation.dashboards').toString(),
      routeProps: {
        as: NavItem,
        name: t('navigation.dashboards'),
        linksTo: '/',
        active: ['/', '/processes/', '/processes/*'],
        breadcrumbsEntities: [{entity: 'report'}],
      },
      isCurrentPage: isCurrentPage(['/', '/processes/', '/processes/*'], pathname),
    },
    {
      key: 'collections',
      label: t('navigation.collections').toString(),
      routeProps: {
        as: NavItem,
        name: t('navigation.collections'),
        linksTo: '/collections',
        active: ['/collections/', '/report/*', '/dashboard/*', '/collection/*'],
        breadcrumbsEntities: [{entity: 'collection'}, {entity: 'dashboard'}, {entity: 'report'}],
      },
      isCurrentPage: isCurrentPage(
        ['/collections/', '/report/*', '/dashboard/*', '/collection/*'],
        pathname
      ),
    },
  ];

  if (optimizeDatabase !== 'opensearch') {
    elements.push({
      key: 'analysis',
      label: t('navigation.analysis').toString(),
      routeProps: {
        as: NavItem,
        name: t('navigation.analysis'),
        linksTo: '/analysis',
        active: ['/analysis/', '/analysis/*'],
      },
      isCurrentPage: isCurrentPage(['/analysis/', '/analysis/*'], pathname),
    });
  }

  const tags: C3NavigationNavBarProps['tags'] = [];

  if (optimizeDatabase === 'opensearch') {
    tags.push({
      key: 'opensearchWarning',
      label: t('navigation.opensearchPreview').toString(),
      tooltip: {
        content: t('navigation.opensearchWarningText').toString(),
        buttonLabel: t('navigation.opensearchPreview').toString(),
      },
      color: 'red',
    });
  }

  const licenseTag: C3NavigationNavBarProps['licenseTag'] = {
    show: licenseType !== 'saas',
    isProductionLicense: validLicense,
  };

  return {
    elements,
    tags,
    licenseTag,
  };
}

function isCurrentPage(active: string[], pathname: string): boolean {
  return matchPath(pathname, {path: active, exact: true}) !== null;
}

function createInfoSideBarProps(
  docsUrl: string,
  enterpriseMode: boolean
): C3NavigationProps['infoSideBar'] {
  return {
    ariaLabel: 'Info',
    elements: [
      {
        key: 'documentation',
        label: t('navigation.documentation').toString(),
        onClick: () => {
          window.open(docsUrl, '_blank');
        },
      },
      {
        key: 'academy',
        label: t('navigation.academy').toString(),
        onClick: () => {
          window.open('https://academy.camunda.com/', '_blank');
        },
      },
      {
        key: 'feedbackAndSupport',
        label: t('navigation.feedback').toString(),
        onClick: () => {
          if (enterpriseMode) {
            window.open('https://jira.camunda.com/projects/SUPPORT/queues', '_blank');
          } else {
            window.open('https://forum.camunda.io/', '_blank');
          }
        },
      },
      {
        key: 'slackCommunityChannel',
        label: 'Slack Community Channel',
        onClick: () => {
          window.open('https://camunda.com/slack', '_blank');
        },
      },
    ],
  };
}

type NavbarWrapperProps = Omit<
  ComponentProps<typeof C3UserConfigurationProvider>,
  'userToken' | 'activeOrganizationId'
> & {
  isCloud: boolean;
  notificationsUrl?: string;
  organizationId?: string;
  userToken?: string | null;
};

function NavbarWrapper({
  isCloud,
  userToken,
  notificationsUrl,
  organizationId,
  children,
}: NavbarWrapperProps) {
  return isCloud && userToken && notificationsUrl && organizationId ? (
    <C3UserConfigurationProvider
      endpoints={{notifications: notificationsUrl}}
      userToken={userToken}
      getNewUserToken={getUserToken}
      activeOrganizationId={organizationId}
    >
      {children}
    </C3UserConfigurationProvider>
  ) : (
    <>{children}</>
  );
}
