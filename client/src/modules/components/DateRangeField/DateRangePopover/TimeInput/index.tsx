/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Field} from 'react-final-form';
import {TextInput} from '@carbon/react';

type Props = {
  type: 'from' | 'to';
  onChange?: () => void;
  labelText: string;
};

const TimeInput: React.FC<Props> = ({type, labelText}) => {
  return (
    <Field name={`${type}Time`}>
      {({input, onChange}) => {
        return (
          <TextInput
            pattern={'\\d{1,2}:\\d{1,2}:\\d{1,2}'}
            defaultValue={input.value}
            id="time-picker"
            labelText={labelText}
            size="sm"
            onChange={(event) => {
              input.onChange(event.target.value);
              onChange?.();
            }}
            placeholder="hh:mm:ss"
            data-testid={`${type}Time`}
            maxLength={8}
            autoComplete="off"
          />
        );
      }}
    </Field>
  );
};

export {TimeInput};
