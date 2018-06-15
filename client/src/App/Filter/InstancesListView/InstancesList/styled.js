import styled from 'styled-components';
import {Colors, themed, themeStyle} from 'modules/theme';

export const InstancesList = styled.div`
  flex-grow: 1;
  position: relative;
`;

export const TableContainer = styled.div`
  position: absolute;
  height: 100%;
  width: 100%;
  left: 0;
  top: 0;
`;

export const SelectionStatusIndicator = themed(styled.div`
  display: inline-block;
  height: 37px;
  width: 8px;
  ${({selected}) =>
    selected &&
    `background-color: ${Colors.selections};`} vertical-align: bottom;
  margin-left: -5px;
  border-right: 1px solid
    ${themeStyle({dark: Colors.uiDark04, light: Colors.uiLight05})};
`);

export const CheckAll = styled.div`
  display: inline-block;
  margin-left: 4px;
  margin-right: 20px;
`;
