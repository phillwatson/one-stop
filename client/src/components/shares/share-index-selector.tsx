import { useEffect, useState } from "react";
import { FormControl,InputLabel, MenuItem, Select } from "@mui/material";

import { useMessageDispatch } from '../../contexts/messages/context';
import ShareService from "../../services/share.service";
import { NULL_INDEX, ShareIndexResponse } from "../../model/share-indices.model";

interface Props {
  onSelect?: (value?: ShareIndexResponse) => void;
}

export default function ShareIndexSelector(props: Props) {
  const showMessage = useMessageDispatch();
  const [ indices, setIndices ] = useState<Array<ShareIndexResponse>>([]);
  const [ index, setIndex ] = useState<ShareIndexResponse>(NULL_INDEX);

  useEffect(() => {
    ShareService.fetchAllIndices()
      .then( response => setIndices(response) )
      .catch(err => {
        setIndices([]);
        showMessage(err);
      })
  }, [ showMessage ]);

  function selectIndex(indexId: string) {
    var selected: ShareIndexResponse | undefined = undefined;
    if (indices !== undefined && indexId !== undefined && indexId.length > 0) {
      selected = indices.find(index => index.id === indexId);
    }

    setIndex(selected || NULL_INDEX);
    if (props.onSelect) {
      props.onSelect(selected);
    }
  }

  return (
      <FormControl fullWidth>
        <InputLabel id="select-indices">Select Index</InputLabel>
        <Select labelId="select-indices" label="Select Index" value={ index.id || ''}
          onChange={(e) => selectIndex(e.target.value as string)}>
          { indices && indices.map(item =>
            <MenuItem value={ item.id } key={ item.id }>{ item.name }</MenuItem>
          )}
        </Select>
      </FormControl>
    
  );
}