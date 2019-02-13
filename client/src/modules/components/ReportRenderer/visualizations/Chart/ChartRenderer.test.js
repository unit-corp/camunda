import React from 'react';
import {shallow} from 'enzyme';

import ChartRenderer from './ChartRenderer';
import Chart from 'chart.js';

const chartData = {
  type: 'visualization_type',
  data: [1, 2]
};

const {WrappedComponent: ChartComponent} = ChartRenderer;

it('should construct a Chart', () => {
  shallow(<ChartComponent config={chartData} />);

  expect(Chart).toHaveBeenCalled();
});

it('should use the provided type for the ChartRenderer', () => {
  Chart.mockClear();

  shallow(<ChartComponent config={chartData} />);

  expect(Chart.mock.calls[0][1].type).toBe('visualization_type');
});
