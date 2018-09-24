import React from 'react';
import {shallow} from 'enzyme';
import {mockResolvedAsyncFn} from 'modules/testUtils';

import Dashboard from './Dashboard';
import MetricPanel from './MetricPanel';
import Header from '../Header';

import * as api from 'modules/api/instances/instances';

api.fetchWorkflowInstancesCount = mockResolvedAsyncFn(123);

describe('Dashboard', () => {
  let node;

  beforeEach(() => {
    node = shallow(<Dashboard />);
  });

  it('should render MetricPanel component', () => {
    expect(node.find(MetricPanel)).toHaveLength(1);
  });

  it('should render Header component', () => {
    // given
    const mockState = {running: 1, incidents: 2};
    node.setState(mockState);
    const headerNode = node.find(Header);

    // then
    expect(headerNode).toHaveLength(1);
    expect(headerNode.prop('active')).toBe('dashboard');
    expect(headerNode.prop('runningInstancesCount')).toBe(mockState.running);
    expect(headerNode.prop('incidentsCount')).toBe(mockState.incidents);
  });

  it('should render three MetricTile components', async () => {
    expect(node.find(MetricPanel).children().length).toBe(3);
  });

  it('it should request instance counts ', async () => {
    // given
    const spyFetch = jest.spyOn(node.instance(), 'fetchCounts');

    // then
    await node.instance().componentDidMount();
    expect(spyFetch).toHaveBeenCalled();
  });
});
