import React, { useContext } from 'react';
import { CircularProgress, TextField } from '@material-ui/core';
import Autocomplete from '@material-ui/lab/Autocomplete';
import axios from 'axios';
import { GlobalDataContext } from "../GlobalDataContext";
import { ResponseRouteSystems } from "../response";

type Props = {
  fieldId: string,
  fieldName: string,
  onChange: Function,
}

export default function SystemInput(props: Props) {
  const globalData = useContext(GlobalDataContext);
  const [open, setOpen] = React.useState(false);
  const [options, setOptions] = React.useState<string[]>([]);
  const loading = open && options.length === 0;

  React.useEffect(() => {
    let active = true;

    if (!loading) {
      return undefined;
    }

    (async () => {
      const response = await axios.get<ResponseRouteSystems>(`${globalData.domain}/api/route/systems`);
      if (active) {
        setOptions(response.data.systems);
      }
    })();

    return () => {
      active = false;
    };
  }, [globalData.domain, loading]);

  React.useEffect(() => {
    if (!open) {
      setOptions([]);
    }
  }, [open]);

  return (
    <Autocomplete
      id={props.fieldId}
      style={{ width: 300 }}
      open={open}
      onOpen={() => {
        setOpen(true);
      }}
      onClose={() => {
        setOpen(false);
      }}
      getOptionSelected={(option, value) => option === value}
      getOptionLabel={(option) => option}
      options={options}
      loading={loading}
      onChange={(_, value) => props.onChange(value)}
      renderInput={(params) => (
        <TextField
          {...params}
          label={props.fieldName}
          variant="outlined"
          InputProps={{
            ...params.InputProps,
            endAdornment: (
              <React.Fragment>
                {loading ? <CircularProgress color="inherit" size={20} /> : null}
                {params.InputProps.endAdornment}
              </React.Fragment>
            ),
          }}
        />
      )}
    />
  );
}
