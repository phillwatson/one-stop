import { useState } from "react";
import { FormControl, InputLabel, MenuItem, Select } from "@mui/material";

import { NULL_INDEX, ShareIndexResponse } from "../../model/share-indices.model";

interface Props {
  shareIndices: ShareIndexResponse[],
  onSelect?: (value?: ShareIndexResponse) => void;
}

export default function ShareIndexSelector(props: Props) {
  const [ index, setIndex ] = useState<ShareIndexResponse>(NULL_INDEX);

  function selectIndex(indexId: string) {
    var selected: ShareIndexResponse | undefined = undefined;
    if (indexId !== undefined && indexId.length > 0) {
      selected = props.shareIndices.find(index => index.id === indexId);
    }

    setIndex(selected || NULL_INDEX);
    if (props.onSelect) {
      props.onSelect(selected);
    }
  }

  return (
      <FormControl fullWidth>
        <InputLabel id="select-index">Select Index</InputLabel>
        <Select labelId="select-index" label="Select Index" value={ index.id || ''}
          onChange={(e) => selectIndex(e.target.value as string)}>
          { props.shareIndices.map(item =>
            <MenuItem value={ item.id } key={ item.id }>{ item.name }</MenuItem>
          )}
        </Select>
      </FormControl>    
  );
}