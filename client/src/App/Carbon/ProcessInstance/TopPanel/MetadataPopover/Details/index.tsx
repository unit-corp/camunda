/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {useState} from 'react';
import {Stack} from '@carbon/react';
import isNil from 'lodash/isNil';
import {MetaDataDto} from 'modules/api/processInstances/fetchFlowNodeMetaData';
import {Link} from 'modules/components/Carbon/Link';
import {Paths} from 'modules/Routes';
import {tracking} from 'modules/tracking';
import {JSONEditorModal} from 'modules/components/Carbon/JSONEditorModal';
import {processInstanceDetailsDiagramStore} from 'modules/stores/processInstanceDetailsDiagram';
import {Header} from '../Header';
import {SummaryDataKey, SummaryDataValue} from '../styled';
import {getExecutionDuration} from './getExecutionDuration';
import {buildMetadata} from './buildMetadata';

type Props = {
  metaData: MetaDataDto;
  flowNodeId: string;
};

const NULL_METADATA = {
  flowNodeInstanceId: null,
  startDate: null,
  endDate: null,
  calledProcessInstanceId: null,
  calledProcessDefinitionName: null,
  calledDecisionInstanceId: null,
  calledDecisionDefinitionName: null,
  flowNodeType: null,
} as const;

const Details: React.FC<Props> = ({metaData, flowNodeId}) => {
  const [isModalVisible, setIsModalVisible] = useState(false);
  const businessObject =
    processInstanceDetailsDiagramStore.businessObjects[flowNodeId];
  const flowNodeName = businessObject?.name || flowNodeId;
  const {instanceMetadata, incident} = metaData;
  const {
    flowNodeInstanceId,
    startDate,
    endDate,
    calledProcessInstanceId,
    calledProcessDefinitionName,
    calledDecisionInstanceId,
    calledDecisionDefinitionName,
    flowNodeType,
  } = instanceMetadata ?? NULL_METADATA;

  return (
    <>
      <Header
        title="Details"
        link={
          !isNil(window.clientConfig?.tasklistUrl) &&
          flowNodeType === 'USER_TASK'
            ? {
                href: window.clientConfig!.tasklistUrl,
                label: 'Open Tasklist',
              }
            : undefined
        }
        button={{
          title: 'Show more metadata',
          label: 'View',
          onClick: () => {
            setIsModalVisible(true);
            tracking.track({
              eventName: 'flow-node-instance-details-opened',
            });
          },
        }}
      />
      <Stack gap={5}>
        <Stack gap={3}>
          <SummaryDataKey>Flow Node Instance Key</SummaryDataKey>
          <SummaryDataValue>{flowNodeInstanceId}</SummaryDataValue>
        </Stack>
        <Stack gap={3}>
          <SummaryDataKey>Execution Duration</SummaryDataKey>
          <SummaryDataValue>
            {getExecutionDuration(startDate!, endDate)}
          </SummaryDataValue>
        </Stack>

        {businessObject?.$type === 'bpmn:CallActivity' &&
          flowNodeType !== 'MULTI_INSTANCE_BODY' && (
            <Stack gap={3}>
              <SummaryDataKey>Called Process Instance</SummaryDataKey>
              <SummaryDataValue data-testid="called-process-instance">
                {calledProcessInstanceId ? (
                  <Link
                    to={Paths.processInstance(calledProcessInstanceId)}
                    title={`View ${calledProcessDefinitionName} instance ${calledProcessInstanceId}`}
                  >
                    {`${calledProcessDefinitionName} - ${calledProcessInstanceId}`}
                  </Link>
                ) : (
                  'None'
                )}
              </SummaryDataValue>
            </Stack>
          )}

        {businessObject?.$type === 'bpmn:BusinessRuleTask' && (
          <Stack gap={3}>
            <SummaryDataKey>Called Decision Instance</SummaryDataKey>
            <SummaryDataValue>
              {calledDecisionInstanceId ? (
                <Link
                  to={Paths.decisionInstance(calledDecisionInstanceId)}
                  title={`View ${calledDecisionDefinitionName} instance ${calledDecisionInstanceId}`}
                >
                  {`${calledDecisionDefinitionName} - ${calledDecisionInstanceId}`}
                </Link>
              ) : (
                calledDecisionDefinitionName ?? '—'
              )}
            </SummaryDataValue>
          </Stack>
        )}
      </Stack>
      <JSONEditorModal
        isVisible={isModalVisible}
        onClose={() => setIsModalVisible(false)}
        title={`Flow Node "${flowNodeName}" ${flowNodeInstanceId} Metadata`}
        value={buildMetadata(instanceMetadata, incident)}
        readOnly
      />
    </>
  );
};

export {Details};
