package de.felser_net.connecttc66c

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import de.felser_net.connecttc66c.databinding.FragmentDeviceCommunicationBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DeviceCommunicationFragment : Fragment() {

    private var _binding: FragmentDeviceCommunicationBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var btComObject: BluetoothCommunication? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDeviceCommunicationBinding.inflate(inflater, container, false)

        if(activity is MainActivity) {
            val activity = activity as MainActivity
            btComObject = activity.btComObject
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGotoBleScan.setOnClickListener {
            findNavController().navigate(R.id.action_DeviceCommunicationFragment_to_BleScanFragment)
        }
        binding.textviewData.text = "<some data>"

        binding.buttonPreviousScreen.setOnClickListener {
            btComObject?.sendCommand(BluetoothCommunication.CMD_PREV_PAGE)
        }
        binding.buttonNextScreen.setOnClickListener {
            btComObject?.sendCommand(BluetoothCommunication.CMD_NEXT_PAGE)
        }
        binding.buttonRotateScreen.setOnClickListener {
            btComObject?.sendCommand(BluetoothCommunication.CMD_ROTATE)
        }
        binding.buttonGetData.setOnClickListener {
            btComObject?.sendCommand(BluetoothCommunication.CMD_GET_VALUES)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        // connect
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val deviceAddress = sharedPref.getString(getString(R.string.saved_device_address), null)
        if (deviceAddress != null)
            btComObject?.connect(deviceAddress)

        // setup our data receiver
        btComObject?.receiveData()
        { data: TC66Data ->
                activity?.runOnUiThread {
                    if(data.decode())
                        showData(data)
                    else
                        binding.textviewData.text = "<decode error>"
                }
        }
    }

    override fun onPause() {
        btComObject?.disconnect()
        super.onPause()
    }

    // this function should run on the ui thread!
    private fun showData(data: TC66Data) {
        binding.textviewProduct.text = data.productName
        binding.textviewSerialnumber.text = getResources().getString(R.string.value_text_serialNumber, data.serialNumber);
        binding.textviewVersion.text = getResources().getString(R.string.value_text_version, data.version);
        binding.textviewNumruns.text = getResources().getString(R.string.value_text_numruns, data.numRuns);

        binding.textviewVoltage.text = getResources().getString(R.string.value_text_voltage, data.voltage);
        binding.textviewCurrent.text = getResources().getString(R.string.value_text_current, data.current);
        binding.textviewPower.text = getResources().getString(R.string.value_text_power, data.power);

        binding.textviewResistance.text = getResources().getString(R.string.value_text_resistance, data.resistance)
        val temperature = data.temperature * (if(data.temperature_sign==0) 1 else -1)
        binding.textviewTemperature.text = getResources().getString(R.string.value_text_temperature, temperature)

        binding.textviewGroup0Charge.text = getResources().getString(R.string.value_text_charge, data.group0_charge)
        binding.textviewGroup0Energy.text = getResources().getString(R.string.value_text_energy, data.group0_energy)
        binding.textviewGroup1Charge.text = getResources().getString(R.string.value_text_charge, data.group1_charge)
        binding.textviewGroup1Energy.text = getResources().getString(R.string.value_text_energy, data.group1_energy)

        binding.textviewDPlusVoltage.text = getResources().getString(R.string.value_text_dplus_voltage, data.d_plus_voltage)
        binding.textviewDMinusVoltage.text = getResources().getString(R.string.value_text_dminus_voltage, data.d_minus_voltage)

        val remainingValues =
            "unknown_1_16 = ${data.unknown_1_16}\n" +
            "unknown_1_20 = ${data.unknown_1_20}\n" +
            "unknown_1_24 = ${data.unknown_1_24}\n" +
            "unknown_1_28 = ${data.unknown_1_28}\n" +
            "unknown_1_32 = ${data.unknown_1_32}\n" +
            "unknown_1_36 = ${data.unknown_1_36}\n" +
            "unknown_1_40 = ${data.unknown_1_40}\n"

        binding.textviewData.text = remainingValues
    }
}